package com.uth.capstone.service;

import com.uth.capstone.dto.request.UpdatePostRequest;
import com.uth.capstone.dto.request.UpdatePostStatusRequest;
import com.uth.capstone.dto.response.PostResponse;
import com.uth.capstone.entity.Category;
import com.uth.capstone.entity.ItemImage;
import com.uth.capstone.entity.ItemPost;
import com.uth.capstone.entity.Location;
import com.uth.capstone.entity.User;
import com.uth.capstone.enums.PostStatus;
import com.uth.capstone.exception.AppException;
import com.uth.capstone.exception.ErrorCode;
import com.uth.capstone.repository.CategoryRepository;
import com.uth.capstone.repository.ItemPostRepository;
import com.uth.capstone.repository.LocationRepository;
import com.uth.capstone.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Service xử lý nghiệp vụ cho Bài đăng Mất đồ / Nhặt được (ItemPost):
 * - Xem chi tiết bài đăng và ẩn số điện thoại cá nhân (PhoneNumber) của người đăng trên dữ liệu công khai.
 * - Chỉ cho phép tác giả chỉnh sửa bài đăng của chính mình khi bài chưa đóng.
 * - Cho phép tác giả xóa bài đăng hoặc đổi trạng thái sang "Đã nhận lại" (RESOLVED / CLAIMED).
 */
@Service
@RequiredArgsConstructor
public class PostService {

    private static final int MAX_IMAGES_PER_POST = 3;

    /**
     * Các trạng thái bài đăng đã đóng (không còn cho phép tác giả chỉnh sửa nội dung).
     */
    private static final Set<PostStatus> CLOSED_STATUSES = EnumSet.of(
            PostStatus.RESOLVED,
            PostStatus.CLAIMED,
            PostStatus.EXPIRED,
            PostStatus.REJECTED
    );

    /**
     * Các trạng thái "Đã nhận lại" mà tác giả được phép chuyển sang qua API PATCH /posts/:id/status.
     */
    private static final Set<PostStatus> ALLOWED_AUTHOR_COMPLETION_STATUSES = EnumSet.of(
            PostStatus.RESOLVED,
            PostStatus.CLAIMED
    );

    private final ItemPostRepository itemPostRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;

    /**
     * Lấy chi tiết bài đăng theo ID và ẩn trường PhoneNumber của người đăng trên dữ liệu công khai.
     */
    @Transactional(readOnly = true)
    public PostResponse getPostById(Integer postId) {
        ItemPost post = itemPostRepository.findWithDetailsByPostId(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        return toPublicPostResponse(post);
    }

    /**
     * Chỉnh sửa bài đăng: chỉ cho phép tác giả chỉnh sửa bài đăng của chính mình khi bài chưa đóng.
     */
    @Transactional
    public PostResponse updatePost(Integer postId, UpdatePostRequest request) {
        CustomUserDetails currentUser = getCurrentUserDetails();

        ItemPost post = itemPostRepository.findWithDetailsByPostId(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        // 1. Kiểm tra quyền sở hữu: Chỉ tác giả của bài đăng mới được phép chỉnh sửa
        verifyPostAuthor(post, currentUser);

        // 2. Kiểm tra trạng thái bài đăng: Chỉ cho phép sửa khi bài chưa đóng
        if (isPostClosed(post.getStatus())) {
            throw new AppException(ErrorCode.POST_ALREADY_CLOSED);
        }

        // 3. Kiểm tra danh mục và địa điểm
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));

        // 4. Cập nhật thông tin bài đăng
        post.setTitle(request.getTitle().trim());
        post.setItemType(request.getItemType());
        post.setCategory(category);
        post.setLocation(location);
        post.setIncidentDate(request.getIncidentDate());
        post.setDescription(request.getDescription().trim());

        // 5. Cập nhật danh sách ảnh đính kèm (nếu client có gửi kèm imageUrls)
        if (request.getImageUrls() != null) {
            List<String> validUrls = request.getImageUrls().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .toList();

            if (validUrls.size() > MAX_IMAGES_PER_POST) {
                throw new AppException(ErrorCode.TOO_MANY_IMAGES);
            }

            post.getImages().clear();
            for (String url : validUrls) {
                ItemImage itemImage = ItemImage.builder()
                        .post(post)
                        .imageUrl(url)
                        .build();
                post.getImages().add(itemImage);
            }
        }

        ItemPost updatedPost = itemPostRepository.save(post);
        return toPublicPostResponse(updatedPost);
    }

    /**
     * Xóa bài đăng: chỉ cho phép tác giả xóa bài đăng của chính mình.
     */
    @Transactional
    public void deletePost(Integer postId) {
        CustomUserDetails currentUser = getCurrentUserDetails();

        ItemPost post = itemPostRepository.findWithDetailsByPostId(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        verifyPostAuthor(post, currentUser);

        itemPostRepository.delete(post);
    }

    /**
     * Cập nhật trạng thái bài đăng sang "Đã nhận lại" (RESOLVED hoặc CLAIMED):
     * chỉ cho phép tác giả thực hiện trên bài đăng của chính mình.
     */
    @Transactional
    public PostResponse updatePostStatusByAuthor(Integer postId, UpdatePostStatusRequest request) {
        CustomUserDetails currentUser = getCurrentUserDetails();

        ItemPost post = itemPostRepository.findWithDetailsByPostId(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        // 1. Chỉ tác giả mới có quyền đổi trạng thái bài đăng của mình
        verifyPostAuthor(post, currentUser);

        // 2. Xác định trạng thái đích (mặc định là RESOLVED nếu không truyền)
        PostStatus targetStatus = (request != null && request.getStatus() != null)
                ? request.getStatus()
                : PostStatus.RESOLVED;

        if (!ALLOWED_AUTHOR_COMPLETION_STATUSES.contains(targetStatus)) {
            throw new AppException(ErrorCode.INVALID_POST_STATUS_TRANSITION);
        }

        // 3. Không cho phép đổi trạng thái nếu bài đăng đã bị từ chối hoặc hết hạn
        if (post.getStatus() == PostStatus.REJECTED || post.getStatus() == PostStatus.EXPIRED) {
            throw new AppException(ErrorCode.POST_ALREADY_CLOSED);
        }

        post.setStatus(targetStatus);
        ItemPost savedPost = itemPostRepository.save(post);
        return toPublicPostResponse(savedPost);
    }

    private void verifyPostAuthor(ItemPost post, CustomUserDetails currentUser) {
        User author = post.getUser();
        if (author == null || !author.getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.NOT_POST_AUTHOR);
        }
    }

    private boolean isPostClosed(PostStatus status) {
        return status != null && CLOSED_STATUSES.contains(status);
    }

    private CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return userDetails;
    }

    /**
     * Chuyển đổi Entity ItemPost sang DTO PostResponse công khai:
     * Ẩn hoàn toàn trường PhoneNumber của người đăng.
     */
    public PostResponse toPublicPostResponse(ItemPost post) {
        User author = post.getUser();
        Category category = post.getCategory();
        Location location = post.getLocation();
        List<ItemImage> itemImages = post.getImages() != null ? post.getImages() : Collections.emptyList();

        PostResponse.AuthorPublicInfo authorInfo = author != null
                ? PostResponse.AuthorPublicInfo.builder()
                        .userId(author.getUserId())
                        .fullName(author.getFullName())
                        .email(author.getEmail())
                        .studentId(author.getStudentId())
                        .role(author.getRole())
                        // Tuyệt đối không gán phoneNumber lên dữ liệu công khai
                        .build()
                : null;

        PostResponse.CategoryInfo categoryInfo = category != null
                ? PostResponse.CategoryInfo.builder()
                        .categoryId(category.getCategoryId())
                        .categoryName(category.getCategoryName())
                        .icon(category.getIcon())
                        .build()
                : null;

        PostResponse.LocationInfo locationInfo = location != null
                ? PostResponse.LocationInfo.builder()
                        .locationId(location.getLocationId())
                        .buildingName(location.getBuildingName())
                        .floor(location.getFloor())
                        .roomZone(location.getRoomZone())
                        .build()
                : null;

        List<PostResponse.ImageInfo> imageInfos = itemImages.stream()
                .map(img -> PostResponse.ImageInfo.builder()
                        .imageId(img.getImageId())
                        .imageUrl(img.getImageUrl())
                        .build())
                .toList();

        List<String> imageUrls = itemImages.stream()
                .map(ItemImage::getImageUrl)
                .toList();

        return PostResponse.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .itemType(post.getItemType())
                .description(post.getDescription())
                .incidentDate(post.getIncidentDate())
                .status(post.getStatus())
                .createdAt(post.getCreatedAt())
                .category(categoryInfo)
                .location(locationInfo)
                .author(authorInfo)
                .images(imageInfos)
                .imageUrls(imageUrls)
                .build();
    }
}

