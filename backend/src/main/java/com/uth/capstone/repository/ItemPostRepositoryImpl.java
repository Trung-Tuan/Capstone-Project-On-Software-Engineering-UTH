package com.uth.capstone.repository;

import com.uth.capstone.dto.request.PostSearchRequest;
import com.uth.capstone.enums.PostSortBy;
import com.uth.capstone.repository.ItemPostRepositoryCustom.PostSearchSlice.PostIdWithScore;
import com.uth.capstone.util.FullTextQueryHelper;
import com.uth.capstone.util.FullTextQueryHelper.ParsedFullTextQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Triển khai truy vấn tối ưu (Deferred Join / Two-Step Query) cho bảng ItemPost (50.000+ bản ghi):
 * - Bước 1: Đếm COUNT(*) và lấy đúng trang PostID (+ relevance_score) trực tiếp trên chỉ mục Full-text / B-tree,
 *           không JOIN bảng ItemImage/User/Category để tránh phình tập kết quả và lỗi sort memory.
 * - Bước 2: Service gọi findAllWithDetailsByPostIdIn(ids) chỉ load chi tiết tối đa `size` bản ghi theo Khóa chính.
 */
@Repository
public class ItemPostRepositoryImpl implements ItemPostRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public PostSearchSlice searchPostIds(PostSearchRequest request) {
        ParsedFullTextQuery parsedKeyword = FullTextQueryHelper.parse(request.getKeyword());
        boolean joinLocation = StringUtils.hasText(request.getBuildingName());
        String fromClause = joinLocation
                ? "FROM `ItemPost` p INNER JOIN `Location` l ON l.`LocationID` = p.`LocationID` "
                : "FROM `ItemPost` p ";

        List<String> baseConditions = new ArrayList<>();
        Map<String, Object> baseParams = new LinkedHashMap<>();
        appendFilterConditions(request, joinLocation, baseConditions, baseParams);

        if (!parsedKeyword.isHasKeyword()) {
            return executeNonKeywordSearch(fromClause, baseConditions, baseParams, request);
        }

        // Đường chính: Sử dụng MySQL FULLTEXT INDEX ft_itempost_search (Title, Description)
        PostSearchSlice fullTextResult = executeFullTextSearch(
                fromClause,
                baseConditions,
                baseParams,
                parsedKeyword.getBooleanQuery(),
                request
        );

        // Fallback an toàn cho từ khóa quá ngắn (< 3 ký tự như "ví", "xe") hoặc toàn bộ là InnoDB stopword
        if (fullTextResult.totalElements() == 0 && !parsedKeyword.isHasIndexableToken()) {
            return executeShortKeywordFallback(
                    fromClause,
                    baseConditions,
                    baseParams,
                    parsedKeyword.getNormalizedPhrase(),
                    request
            );
        }

        return fullTextResult;
    }

    private void appendFilterConditions(
            PostSearchRequest request,
            boolean joinLocation,
            List<String> conditions,
            Map<String, Object> params
    ) {
        if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
            conditions.add("p.`Status` IN (:statuses)");
            params.put("statuses", request.getStatuses().stream().map(Enum::name).toList());
        }

        if (request.getItemType() != null) {
            conditions.add("p.`ItemType` = :itemType");
            params.put("itemType", request.getItemType().name());
        }

        if (request.getCategoryId() != null) {
            conditions.add("p.`CategoryID` = :categoryId");
            params.put("categoryId", request.getCategoryId());
        }

        if (request.getLocationId() != null) {
            conditions.add("p.`LocationID` = :locationId");
            params.put("locationId", request.getLocationId());
        }

        if (joinLocation) {
            conditions.add("LOWER(l.`BuildingName`) LIKE LOWER(:buildingPattern) ESCAPE '!'");
            params.put(
                    "buildingPattern",
                    "%" + FullTextQueryHelper.escapeLikePattern(request.getBuildingName().trim()) + "%"
            );
        }

        if (request.getFromDateTime() != null) {
            conditions.add("p.`IncidentDate` >= :fromDateTime");
            params.put("fromDateTime", request.getFromDateTime());
        }

        if (request.getToDateTime() != null) {
            String operator = request.isToDateExclusive() ? "<" : "<=";
            conditions.add("p.`IncidentDate` " + operator + " :toDateTime");
            params.put("toDateTime", request.getToDateTime());
        }
    }

    private PostSearchSlice executeNonKeywordSearch(
            String fromClause,
            List<String> conditions,
            Map<String, Object> params,
            PostSearchRequest request
    ) {
        String whereClause = buildWhereClause(conditions);
        long total = executeCountQuery(fromClause + whereClause, params);

        int offset = request.getPage() * request.getSize();
        if (total == 0 || offset >= total) {
            return new PostSearchSlice(List.of(), total);
        }

        String orderByClause = resolveOrderBy(request.getSortBy(), false);
        String selectSql = "SELECT p.`PostID` " + fromClause + whereClause + orderByClause;

        Query selectQuery = entityManager.createNativeQuery(selectSql);
        bindParameters(selectQuery, params);
        selectQuery.setFirstResult(offset);
        selectQuery.setMaxResults(request.getSize());

        @SuppressWarnings("unchecked")
        List<Object> rawIds = selectQuery.getResultList();
        List<PostIdWithScore> rows = rawIds.stream()
                .map(id -> new PostIdWithScore(((Number) id).intValue(), null))
                .toList();

        return new PostSearchSlice(rows, total);
    }

    private PostSearchSlice executeFullTextSearch(
            String fromClause,
            List<String> baseConditions,
            Map<String, Object> baseParams,
            String booleanQuery,
            PostSearchRequest request
    ) {
        List<String> conditions = new ArrayList<>(baseConditions);
        conditions.add("MATCH(p.`Title`, p.`Description`) AGAINST(:ftQuery IN BOOLEAN MODE)");

        Map<String, Object> params = new LinkedHashMap<>(baseParams);
        params.put("ftQuery", booleanQuery);

        String whereClause = buildWhereClause(conditions);
        long total = executeCountQuery(fromClause + whereClause, params);

        int offset = request.getPage() * request.getSize();
        if (total == 0 || offset >= total) {
            return new PostSearchSlice(List.of(), total);
        }

        String orderByClause = resolveOrderBy(request.getSortBy(), true);
        String selectSql = "SELECT p.`PostID`, "
                + "MATCH(p.`Title`, p.`Description`) AGAINST(:ftQuery IN BOOLEAN MODE) AS relevance_score "
                + fromClause
                + whereClause
                + orderByClause;

        Query selectQuery = entityManager.createNativeQuery(selectSql);
        bindParameters(selectQuery, params);
        selectQuery.setFirstResult(offset);
        selectQuery.setMaxResults(request.getSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rawRows = selectQuery.getResultList();
        List<PostIdWithScore> rows = rawRows.stream()
                .map(row -> new PostIdWithScore(
                        ((Number) row[0]).intValue(),
                        row[1] != null ? ((Number) row[1]).doubleValue() : 0.0
                ))
                .toList();

        return new PostSearchSlice(rows, total);
    }

    private PostSearchSlice executeShortKeywordFallback(
            String fromClause,
            List<String> baseConditions,
            Map<String, Object> baseParams,
            String normalizedPhrase,
            PostSearchRequest request
    ) {
        List<String> tokens = FullTextQueryHelper.extractTokens(normalizedPhrase);
        if (tokens.isEmpty()) {
            return new PostSearchSlice(List.of(), 0L);
        }

        List<String> conditions = new ArrayList<>(baseConditions);
        Map<String, Object> params = new LinkedHashMap<>(baseParams);

        for (int i = 0; i < tokens.size(); i++) {
            String paramName = "shortToken" + i;
            conditions.add("(LOWER(p.`Title`) LIKE LOWER(:" + paramName + ") ESCAPE '!' "
                    + "OR LOWER(p.`Description`) LIKE LOWER(:" + paramName + ") ESCAPE '!')");
            params.put(paramName, "%" + FullTextQueryHelper.escapeLikePattern(tokens.get(i)) + "%");
        }

        String whereClause = buildWhereClause(conditions);
        long total = executeCountQuery(fromClause + whereClause, params);

        int offset = request.getPage() * request.getSize();
        if (total == 0 || offset >= total) {
            return new PostSearchSlice(List.of(), total);
        }

        String phraseParam = "%" + FullTextQueryHelper.escapeLikePattern(normalizedPhrase) + "%";
        params.put("shortPhrasePattern", phraseParam);

        String orderByClause = resolveOrderBy(request.getSortBy(), true);
        String selectSql = "SELECT p.`PostID`, "
                + "(CASE "
                + "WHEN LOWER(p.`Title`) LIKE LOWER(:shortPhrasePattern) ESCAPE '!' THEN 3.0 "
                + "WHEN LOWER(p.`Title`) LIKE LOWER(:shortToken0) ESCAPE '!' THEN 2.0 "
                + "ELSE 1.0 END) AS relevance_score "
                + fromClause
                + whereClause
                + orderByClause;

        Query selectQuery = entityManager.createNativeQuery(selectSql);
        bindParameters(selectQuery, params);
        selectQuery.setFirstResult(offset);
        selectQuery.setMaxResults(request.getSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rawRows = selectQuery.getResultList();
        List<PostIdWithScore> rows = rawRows.stream()
                .map(row -> new PostIdWithScore(
                        ((Number) row[0]).intValue(),
                        row[1] != null ? ((Number) row[1]).doubleValue() : 1.0
                ))
                .toList();

        return new PostSearchSlice(rows, total);
    }

    private long executeCountQuery(String fromAndWhereClause, Map<String, Object> params) {
        String countSql = "SELECT COUNT(*) " + fromAndWhereClause;
        Query countQuery = entityManager.createNativeQuery(countSql);
        bindParameters(countQuery, params);
        Object singleResult = countQuery.getSingleResult();
        return ((Number) singleResult).longValue();
    }

    private String buildWhereClause(List<String> conditions) {
        if (conditions.isEmpty()) {
            return "";
        }
        return "WHERE " + String.join(" AND ", conditions) + " ";
    }

    private String resolveOrderBy(PostSortBy sortBy, boolean hasKeyword) {
        PostSortBy effectiveSort = sortBy != null ? sortBy : PostSortBy.NEWEST;
        return switch (effectiveSort) {
            case OLDEST -> "ORDER BY p.`CreatedAt` ASC, p.`PostID` ASC";
            case RELEVANCE -> hasKeyword
                    ? "ORDER BY relevance_score DESC, p.`CreatedAt` DESC, p.`PostID` DESC"
                    : "ORDER BY p.`CreatedAt` DESC, p.`PostID` DESC";
            case NEWEST -> "ORDER BY p.`CreatedAt` DESC, p.`PostID` DESC";
        };
    }

    private void bindParameters(Query query, Map<String, Object> params) {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
    }
}
