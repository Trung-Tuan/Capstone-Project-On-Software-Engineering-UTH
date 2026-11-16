package com.uth.capstone.util;

import lombok.Getter;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tiện ích chuẩn hóa từ khóa cho MySQL Full-text Search (IN BOOLEAN MODE):
 * - Loại bỏ các ký tự điều khiển (+, -, *, >, <, ~, ", @, (, )) để tránh lỗi cú pháp MySQL 1064.
 * - Nhận diện danh sách stopword mặc định của InnoDB (kể cả từ tiếng Việt bị gộp dấu theo utf8mb4_unicode_ci như "ăn", "cơm", "để", "là")
 *   để tránh gắn toán tử '+' làm truy vấn trả về rỗng.
 * - Tự động nhận biết trường hợp từ khóa chỉ gồm từ quá ngắn (< 3 ký tự như "ví", "xe") để có cơ chế fallback an toàn.
 */
public final class FullTextQueryHelper {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}\\p{N}]+", Pattern.UNICODE_CHARACTER_CLASS);

    /**
     * Danh sách 35 từ dừng mặc định của MySQL InnoDB Full-Text (INNODB_FT_DEFAULT_STOPWORD).
     */
    private static final Set<String> INNODB_DEFAULT_STOPWORDS = Set.of(
            "a", "about", "an", "are", "as", "at", "be", "by", "com", "de", "en",
            "for", "from", "how", "i", "in", "is", "it", "la", "of", "on", "or",
            "that", "the", "this", "to", "was", "what", "when", "where", "who",
            "will", "with", "und", "www"
    );

    private FullTextQueryHelper() {
    }

    public static ParsedFullTextQuery parse(String rawKeyword) {
        if (!StringUtils.hasText(rawKeyword)) {
            return ParsedFullTextQuery.empty();
        }

        String trimmed = rawKeyword.trim();
        List<String> tokens = extractTokens(trimmed);
        if (tokens.isEmpty()) {
            return ParsedFullTextQuery.empty();
        }

        StringBuilder booleanBuilder = new StringBuilder();
        boolean hasIndexableToken = false;

        for (String token : tokens) {
            if (!booleanBuilder.isEmpty()) {
                booleanBuilder.append(' ');
            }

            if (token.length() >= 3 && !isInnoDbStopword(token)) {
                // Từ >= 3 ký tự và không phải stopword: bắt buộc xuất hiện (AND) + hỗ trợ tìm tiền tố (*)
                booleanBuilder.append('+').append(token).append('*');
                hasIndexableToken = true;
            } else {
                // Từ ngắn (< 3 ký tự) hoặc stopword: không gắn '+' để tránh MySQL InnoDB FTS trả về 0 kết quả
                booleanBuilder.append(token).append('*');
            }
        }

        String normalizedPhrase = String.join(" ", tokens);
        return new ParsedFullTextQuery(true, booleanBuilder.toString(), normalizedPhrase, hasIndexableToken);
    }

    public static List<String> extractTokens(String input) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(input);
        while (matcher.find()) {
            String token = matcher.group().toLowerCase(Locale.ROOT);
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    public static boolean isInnoDbStopword(String token) {
        String asciiFolded = stripVietnameseAccents(token.toLowerCase(Locale.ROOT));
        return INNODB_DEFAULT_STOPWORDS.contains(asciiFolded);
    }

    public static String stripVietnameseAccents(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
    }

    public static String escapeLikePattern(String input) {
        return input
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    @Getter
    public static final class ParsedFullTextQuery {
        private final boolean hasKeyword;
        private final String booleanQuery;
        private final String normalizedPhrase;
        private final boolean hasIndexableToken;

        private ParsedFullTextQuery(
                boolean hasKeyword,
                String booleanQuery,
                String normalizedPhrase,
                boolean hasIndexableToken
        ) {
            this.hasKeyword = hasKeyword;
            this.booleanQuery = booleanQuery;
            this.normalizedPhrase = normalizedPhrase;
            this.hasIndexableToken = hasIndexableToken;
        }

        public static ParsedFullTextQuery empty() {
            return new ParsedFullTextQuery(false, "", "", false);
        }
    }
}
