package com.uth.capstone.enums;

import com.uth.capstone.exception.AppException;
import com.uth.capstone.exception.ErrorCode;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Kiểu sắp xếp danh sách bài đăng (FR-SRCH-03).
 */
@Getter
public enum PostSortBy {
    NEWEST("Mới nhất"),
    OLDEST("Cũ nhất"),
    RELEVANCE("Độ liên quan của từ khóa");

    private final String description;

    PostSortBy(String description) {
        this.description = description;
    }

    /**
     * Phân tích chuỗi sortBy từ query param (hỗ trợ cả alias thân thiện).
     */
    public static PostSortBy fromString(String value, boolean hasKeyword) {
        if (!StringUtils.hasText(value)) {
            return hasKeyword ? RELEVANCE : NEWEST;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "NEWEST", "LATEST", "CREATED_AT_DESC", "DESC" -> NEWEST;
            case "OLDEST", "EARLIEST", "CREATED_AT_ASC", "ASC" -> OLDEST;
            case "RELEVANCE", "RELEVANT", "SCORE" -> RELEVANCE;
            default -> throw new AppException(ErrorCode.INVALID_FILTER_PARAMETER);
        };
    }
}
