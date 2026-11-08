package com.uth.capstone.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.uth.capstone.enums.ItemType;
import com.uth.capstone.enums.PostStatus;
import com.uth.capstone.enums.Role;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO trả về thông tin chi tiết bài đăng công khai.
 * Trường số điện thoại cá nhân (PhoneNumber) của người đăng được ẩn hoàn toàn trên dữ liệu công khai.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostResponse {

    private Integer postId;
    private String title;
    private ItemType itemType;
    private String description;
    private LocalDateTime incidentDate;
    private PostStatus status;
    private LocalDateTime createdAt;

    private CategoryInfo category;
    private LocationInfo location;
    private AuthorPublicInfo author;
    private List<ImageInfo> images;
    private List<String> imageUrls;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CategoryInfo {
        private Integer categoryId;
        private String categoryName;
        private String icon;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LocationInfo {
        private Integer locationId;
        private String buildingName;
        private String floor;
        private String roomZone;
    }

    /**
     * Thông tin công khai của người đăng bài.
     * Tuyệt đối ẩn trường số điện thoại cá nhân (phoneNumber) trên dữ liệu công khai (FR-POST / Bảo mật thông tin).
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AuthorPublicInfo {
        private Integer userId;
        private String fullName;
        private String email;
        private String studentId;
        private Role role;

        @JsonIgnore
        private String phoneNumber;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageInfo {
        private Integer imageId;
        private String imageUrl;
    }
}

