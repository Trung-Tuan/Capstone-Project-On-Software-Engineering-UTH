package com.uth.capstone.repository;

import com.uth.capstone.dto.request.PostSearchRequest;

import java.util.List;

/**
 * Custom repository tối ưu hiệu năng tìm kiếm Full-text và bộ lọc kết hợp 4 tiêu chí trên bảng ItemPost.
 */
public interface ItemPostRepositoryCustom {

    PostSearchSlice searchPostIds(PostSearchRequest request);

    record PostSearchSlice(
            List<PostIdWithScore> rows,
            long totalElements
    ) {
        public record PostIdWithScore(Integer postId, Double relevanceScore) {
        }
    }
}
