package com.uth.capstone.service;

import com.uth.capstone.dto.request.CreatePostRequest;
import com.uth.capstone.dto.request.PostSearchRequest;
import com.uth.capstone.dto.request.UpdatePostRequest;
import com.uth.capstone.dto.request.UpdatePostStatusRequest;
import com.uth.capstone.dto.response.PageResponse;
import com.uth.capstone.dto.response.PostResponse;
import com.uth.capstone.entity.Category;
import com.uth.capstone.entity.ItemImage;
import com.uth.capstone.entity.ItemPost;
import com.uth.capstone.entity.Location;
import com.uth.capstone.entity.User;
import com.uth.capstone.enums.ItemType;
import com.uth.capstone.enums.PostFilterStatus;
import com.uth.capstone.enums.PostSortBy;
import com.uth.capstone.enums.PostStatus;
import com.uth.capstone.enums.Role;
import com.uth.capstone.enums.UserStatus;
import com.uth.capstone.exception.AppException;
import com.uth.capstone.exception.ErrorCode;
import com.uth.capstone.repository.CategoryRepository;
import com.uth.capstone.repository.ItemPostRepository;
import com.uth.capstone.repository.ItemPostRepositoryCustom.PostSearchSlice;
import com.uth.capstone.repository.LocationRepository;
import com.uth.capstone.repository.UserRepository;
import com.uth.capstone.security.CustomUserDetails;
import com.uth.capstone.util.FullTextQueryHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service xử lý nghiệp vụ cho Bài đăng Mất đồ / Nhặt được (ItemPost):
 * - Tạo mới bài đăng (kiểm tra danh mục, địa điểm, tối đa 3 ảnh, gán trạng thái PENDING hoặc APPROVED).
 * - Tìm kiếm Full-text trên Title & Description, bộ lọc kết hợp 4 tiêu chí & sắp xếp tối ưu < 1.5s với 50.000+ bản ghi.
 * - Xem chi tiết bài đăng và ẩn số điện thoại cá nhân (PhoneNumber) của người đăng trên dữ liệu công khai.
 * - Chỉ cho phép tác giả chỉnh sửa bài đăng của chính mình khi bài chưa đóng.
 * - Cho phép tác giả xóa bài đăng hoặc đổi trạng thái sang "Đã nhận lại" (RESOLVED / CLAIMED).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private static final int MAX_IMAGES_PER_POST = 3;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_IMAGE_URL_LENGTH = 500;

    /**
     * Các vai trò quản trị/bảo vệ/CTSV được phép tự động duyệt hiển thị bài đăng khi tạo mới.
     */
    private static final Set<Role> AUTO_APPROVED_ROLES = EnumSet.of(
            Role.ADMIN,
            Role.SECURITY,
            Role.SAC
    );

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
    private final UserRepository userRepository;

    @Value("${app.post.auto-approve:false}")
    private boolean autoApprove;

    /**
     * Tạo mới bài đăng Mất đồ / Nhặt được:
     * Kiểm tra danh mục, địa điểm, giới hạn ảnh đính kèm (tối đa 3 ảnh)
     * và lưu vào CSDL ở trạng thái PENDING (chờ duyệt) hoặc APPROVED (nếu tự động duyệt).
     */
    @Transactional
    public PostResponse createPost(CreatePostRequest request) {
        CustomUserDetails currentUser = getCurrentUserDetails();

        User author = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (author.getStatus() == UserStatus.LOCKED) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        Category category = findCategoryById(request.getCategoryId());
        Location location = findLocationById(request.getLocationId());

        ItemPost post = ItemPost.builder()
                .user(author)
                .title(request.getTitle().trim())
                .itemType(request.getItemType())
                .category(category)
                .location(location)
                .incidentDate(request.getIncidentDate())
                .description(request.getDescription().trim())
                .status(resolveInitialPostStatus(author))
                .images(new ArrayList<>())
                .build();

        if (request.getImageUrls() != null) {
            attachPostImages(post, request.getImageUrls(), false);
        }

        ItemPost savedPost = itemPostRepository.save(post);
        return toPublicPostResponse(savedPost);
    }

    /**
     * Tìm kiếm Full-text trên Title & Description kết hợp bộ lọc 4 tiêu chí:
     * 1. Trạng thái (Cần tìm / Nhặt được / Đã hoàn trả)
     * 2. Danh mục (CategoryID)
     * 3. Khu vực / Tòa nhà (LocationID / BuildingName)
     * 4. Khoảng thời gian (Từ ngày - Đến ngày)
     * Và hỗ trợ sắp xếp theo Mới nhất, Cũ nhất, Độ liên quan của từ khóa.
     */
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> searchPosts(
            String keyword,
            String status,
            String itemType,
            Integer categoryId,
            Integer locationId,
            String buildingName,
            String fromDate,
            String toDate,
            String sortBy,
            int page,
            int size
    ) {
        long startNs = System.nanoTime();

        PostSearchRequest searchRequest = buildSearchRequest(
                keyword, status, itemType, categoryId, locationId, buildingName,
                fromDate, toDate, sortBy, page, size
        );

        PostSearchSlice slice = itemPostRepository.searchPostIds(searchRequest);

        List<PostResponse> items;
        if (slice.rows().isEmpty()) {
            items = Collections.emptyList();
        } else {
            List<Integer> orderedIds = slice.rows().stream()
                    .map(PostSearchSlice.PostIdWithScore::postId)
                    .toList();

            Map<Integer, ItemPost> postMap = itemPostRepository.findAllWithDetailsByPostIdIn(orderedIds)
                    .stream()
                    .collect(Collectors.toMap(ItemPost::getPostId, Function.identity(), (a, b) -> a));

            items = slice.rows().stream()
                    .map(row -> {
                        ItemPost post = postMap.get(row.postId());
                        if (post == null) {
                            return null;
                        }
                        PostResponse dto = toPublicPostResponse(post);
                        dto.setRelevanceScore(row.relevanceScore());
                        return dto;
                    })
                    .filter(Objects::nonNull)
                    .toList();
        }

        long totalElements = slice.totalElements();
        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil((double) totalElements / searchRequest.getSize());
        long queryTimeMs = (System.nanoTime() - startNs) / 1_000_000L;

        log.debug("GET /posts executed in {} ms (totalElements={}, page={}, size={}, sortBy={})",
                queryTimeMs, totalElements, searchRequest.getPage(), searchRequest.getSize(), searchRequest.getSortBy());

        return PageResponse.<PostResponse>builder()
                .items(items)
                .page(searchRequest.getPage())
                .size(searchRequest.getSize())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(searchRequest.getPage() + 1 < totalPages)
                .hasPrevious(searchRequest.getPage() > 0 && totalPages > 0)
                .sortBy(searchRequest.getSortBy().name())
                .queryTimeMs(queryTimeMs)
                .build();
    }

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
        Category category = findCategoryById(request.getCategoryId());
        Location location = findLocationById(request.getLocationId());

        // 4. Cập nhật thông tin bài đăng
        post.setTitle(request.getTitle().trim());
        post.setItemType(request.getItemType());
        post.setCategory(category);
        post.setLocation(location);
        post.setIncidentDate(request.getIncidentDate());
        post.setDescription(request.getDescription().trim());

        // 5. Cập nhật danh sách ảnh đính kèm (nếu client có gửi kèm imageUrls)
        if (request.getImageUrls() != null) {
            attachPostImages(post, request.getImageUrls(), true);
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

    private PostSearchRequest buildSearchRequest(
            String keyword,
            String status,
            String itemType,
            Integer categoryId,
            Integer locationId,
            String buildingName,
            String fromDate,
            String toDate,
            String sortBy,
            int page,
            int size
    ) {
        String sanitizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        boolean hasKeyword = FullTextQueryHelper.parse(sanitizedKeyword).isHasKeyword();

        PostFilterStatus filterStatus = PostFilterStatus.fromString(status);
        ItemType parsedItemType = parseItemType(itemType);

        List<PostStatus> resolvedStatuses;
        ItemType resolvedItemType;

        if (filterStatus == null) {
            resolvedStatuses = PostFilterStatus.DEFAULT_PUBLIC_STATUSES;
            resolvedItemType = parsedItemType;
        } else if (filterStatus == PostFilterStatus.LOST) {
            if (parsedItemType == ItemType.FOUND) {
                throw new AppException(ErrorCode.INVALID_FILTER_PARAMETER);
            }
            resolvedItemType = ItemType.LOST;
            resolvedStatuses = List.of(PostStatus.APPROVED);
        } else if (filterStatus == PostFilterStatus.FOUND) {
            if (parsedItemType == ItemType.LOST) {
                throw new AppException(ErrorCode.INVALID_FILTER_PARAMETER);
            }
            resolvedItemType = ItemType.FOUND;
            resolvedStatuses = List.of(PostStatus.APPROVED);
        } else if (filterStatus == PostFilterStatus.RETURNED) {
            resolvedStatuses = PostFilterStatus.RETURNED_STATUSES;
            resolvedItemType = parsedItemType;
        } else if (filterStatus == PostFilterStatus.ALL) {
            resolvedStatuses = null;
            resolvedItemType = parsedItemType;
        } else {
            resolvedStatuses = List.of(PostStatus.valueOf(filterStatus.name()));
            resolvedItemType = parsedItemType;
        }

        LocalDateTime fromDateTime = parseStartDateTime(fromDate);
        ParsedEndDate parsedEndDate = parseEndDateTime(toDate);

        validateDateRange(fromDateTime, parsedEndDate);

        PostSortBy parsedSortBy = PostSortBy.fromString(sortBy, hasKeyword);
        PostSortBy effectiveSortBy = (parsedSortBy == PostSortBy.RELEVANCE && !hasKeyword)
                ? PostSortBy.NEWEST
                : parsedSortBy;

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        return PostSearchRequest.builder()
                .keyword(sanitizedKeyword)
                .statuses(resolvedStatuses)
                .itemType(resolvedItemType)
                .categoryId(categoryId)
                .locationId(locationId)
                .buildingName(StringUtils.hasText(buildingName) ? buildingName.trim() : null)
                .fromDateTime(fromDateTime)
                .toDateTime(parsedEndDate.dateTime())
                .toDateExclusive(parsedEndDate.exclusive())
                .sortBy(effectiveSortBy)
                .page(safePage)
                .size(safeSize)
                .build();
    }

    private ItemType parseItemType(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return ItemType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.INVALID_FILTER_PARAMETER);
        }
    }

    private LocalDateTime parseStartDateTime(String rawDate) {
        if (!StringUtils.hasText(rawDate)) {
            return null;
        }
        String trimmed = rawDate.trim();
        try {
            if (trimmed.length() == 10) {
                return LocalDate.parse(trimmed).atStartOfDay();
            }
            return LocalDateTime.parse(trimmed);
        } catch (DateTimeParseException ex) {
            throw new AppException(ErrorCode.INVALID_FILTER_PARAMETER);
        }
    }

    private ParsedEndDate parseEndDateTime(String rawDate) {
        if (!StringUtils.hasText(rawDate)) {
            return new ParsedEndDate(null, false);
        }
        String trimmed = rawDate.trim();
        try {
            if (trimmed.length() == 10) {
                // Cộng 1 ngày và dùng '<' để bao trọn tới 23:59:59, tránh lỗi làm tròn DATETIME(0) trong MySQL
                return new ParsedEndDate(LocalDate.parse(trimmed).plusDays(1).atStartOfDay(), true);
            }
            return new ParsedEndDate(LocalDateTime.parse(trimmed), false);
        } catch (DateTimeParseException ex) {
            throw new AppException(ErrorCode.INVALID_FILTER_PARAMETER);
        }
    }

    private void validateDateRange(LocalDateTime fromDateTime, ParsedEndDate endDate) {
        if (fromDateTime == null || endDate.dateTime() == null) {
            return;
        }
        if (endDate.exclusive()) {
            if (!fromDateTime.isBefore(endDate.dateTime())) {
                throw new AppException(ErrorCode.INVALID_DATE_RANGE);
            }
        } else if (fromDateTime.isAfter(endDate.dateTime())) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    private Category findCategoryById(Integer categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private Location findLocationById(Integer locationId) {
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));
    }

    private PostStatus resolveInitialPostStatus(User author) {
        if (autoApprove || (author.getRole() != null && AUTO_APPROVED_ROLES.contains(author.getRole()))) {
            return PostStatus.APPROVED;
        }
        return PostStatus.PENDING;
    }

    private void attachPostImages(ItemPost post, List<String> imageUrls, boolean replaceExisting) {
        List<String> validUrls = imageUrls.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();

        if (validUrls.size() > MAX_IMAGES_PER_POST) {
            throw new AppException(ErrorCode.TOO_MANY_IMAGES);
        }

        for (String url : validUrls) {
            if (url.length() > MAX_IMAGE_URL_LENGTH) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
        }

        if (replaceExisting) {
            post.getImages().clear();
        }

        for (String url : validUrls) {
            ItemImage itemImage = ItemImage.builder()
                    .post(post)
                    .imageUrl(url)
                    .build();
            post.getImages().add(itemImage);
        }
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

    private record ParsedEndDate(LocalDateTime dateTime, boolean exclusive) {
    }
}
