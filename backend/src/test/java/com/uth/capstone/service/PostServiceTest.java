package com.uth.capstone.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uth.capstone.dto.request.PostSearchRequest;
import com.uth.capstone.dto.request.UpdatePostRequest;
import com.uth.capstone.dto.request.UpdatePostStatusRequest;
import com.uth.capstone.dto.response.PageResponse;
import com.uth.capstone.dto.response.PostResponse;
import com.uth.capstone.entity.Category;
import com.uth.capstone.entity.ItemPost;
import com.uth.capstone.entity.Location;
import com.uth.capstone.entity.User;
import com.uth.capstone.enums.ItemType;
import com.uth.capstone.enums.PostSortBy;
import com.uth.capstone.enums.PostStatus;
import com.uth.capstone.enums.Role;
import com.uth.capstone.enums.UserStatus;
import com.uth.capstone.exception.AppException;
import com.uth.capstone.exception.ErrorCode;
import com.uth.capstone.repository.CategoryRepository;
import com.uth.capstone.repository.ItemPostRepository;
import com.uth.capstone.repository.ItemPostRepositoryCustom.PostSearchSlice;
import com.uth.capstone.repository.ItemPostRepositoryCustom.PostSearchSlice.PostIdWithScore;
import com.uth.capstone.repository.LocationRepository;
import com.uth.capstone.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostServiceTest {

    private ItemPostRepository itemPostRepository;
    private CategoryRepository categoryRepository;
    private LocationRepository locationRepository;
    private PostService postService;

    private User authorUser;
    private User otherUser;
    private Category category;
    private Location location;
    private ItemPost samplePost;
    private ItemPost secondPost;

    @BeforeEach
    void setUp() {
        itemPostRepository = Mockito.mock(ItemPostRepository.class);
        categoryRepository = Mockito.mock(CategoryRepository.class);
        locationRepository = Mockito.mock(LocationRepository.class);
        postService = new PostService(itemPostRepository, categoryRepository, locationRepository);

        authorUser = User.builder()
                .userId(1)
                .email("2251120001@ut.edu.vn")
                .fullName("Nguyễn Văn A")
                .studentId("2251120001")
                .phoneNumber("0901234567")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .build();

        otherUser = User.builder()
                .userId(2)
                .email("2251120002@ut.edu.vn")
                .fullName("Trần Thị B")
                .studentId("2251120002")
                .phoneNumber("0909999999")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .build();

        category = Category.builder()
                .categoryId(1)
                .categoryName("Ví / Bóp")
                .icon("wallet")
                .build();

        location = Location.builder()
                .locationId(1)
                .buildingName("Tòa nhà A")
                .floor("Tầng 2")
                .roomZone("Phòng A201")
                .build();

        samplePost = ItemPost.builder()
                .postId(100)
                .user(authorUser)
                .title("Mất ví da màu đen")
                .itemType(ItemType.LOST)
                .category(category)
                .location(location)
                .incidentDate(LocalDateTime.of(2026, 10, 25, 10, 0))
                .description("Mất ví da màu đen có thẻ sinh viên")
                .status(PostStatus.APPROVED)
                .createdAt(LocalDateTime.of(2026, 10, 25, 12, 0))
                .images(new ArrayList<>())
                .build();

        secondPost = ItemPost.builder()
                .postId(101)
                .user(otherUser)
                .title("Nhặt được ví da đen ở thư viện")
                .itemType(ItemType.FOUND)
                .category(category)
                .location(location)
                .incidentDate(LocalDateTime.of(2026, 10, 26, 14, 0))
                .description("Đang giữ tại phòng bảo vệ")
                .status(PostStatus.RESOLVED)
                .createdAt(LocalDateTime.of(2026, 10, 26, 15, 0))
                .images(new ArrayList<>())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /posts kết hợp 4 tiêu chí lọc (status=LOST, categoryId, locationId, fromDate-toDate) và ẩn PhoneNumber")
    void shouldSearchPostsWithAllFourFiltersAndHidePhoneNumber() throws Exception {
        when(itemPostRepository.searchPostIds(any(PostSearchRequest.class)))
                .thenReturn(new PostSearchSlice(List.of(new PostIdWithScore(100, 1.85)), 1L));
        when(itemPostRepository.findAllWithDetailsByPostIdIn(List.of(100)))
                .thenReturn(List.of(samplePost));

        PageResponse<PostResponse> response = postService.searchPosts(
                "ví da",
                "LOST",
                null,
                1,
                1,
                null,
                "2026-10-01",
                "2026-10-31",
                "RELEVANCE",
                0,
                12
        );

        ArgumentCaptor<PostSearchRequest> captor = ArgumentCaptor.forClass(PostSearchRequest.class);
        verify(itemPostRepository).searchPostIds(captor.capture());

        PostSearchRequest captured = captor.getValue();
        assertEquals("ví da", captured.getKeyword());
        assertEquals(ItemType.LOST, captured.getItemType());
        assertEquals(List.of(PostStatus.APPROVED), captured.getStatuses());
        assertEquals(1, captured.getCategoryId());
        assertEquals(1, captured.getLocationId());
        assertEquals(LocalDateTime.of(2026, 10, 1, 0, 0), captured.getFromDateTime());
        assertEquals(LocalDateTime.of(2026, 11, 1, 0, 0), captured.getToDateTime());
        assertTrue(captured.isToDateExclusive());
        assertEquals(PostSortBy.RELEVANCE, captured.getSortBy());

        assertEquals(1, response.getItems().size());
        assertEquals(1.85, response.getItems().get(0).getRelevanceScore());
        assertNull(response.getItems().get(0).getAuthor().getPhoneNumber());

        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        String json = mapper.writeValueAsString(response);
        assertFalse(json.contains("0901234567"));
        assertFalse(json.contains("phoneNumber"));
    }

    @Test
    @DisplayName("GET /posts hỗ trợ lọc trạng thái Đã hoàn trả (status=RETURNED -> RESOLVED/CLAIMED) và sắp xếp giữ đúng thứ tự")
    void shouldFilterByReturnedStatusAndPreserveRelevanceOrder() {
        // Giả lập DB trả về thứ tự postId 101 trước 100, dù findAllWithDetailsByPostIdIn trả về ngược lại [100, 101]
        when(itemPostRepository.searchPostIds(any(PostSearchRequest.class)))
                .thenReturn(new PostSearchSlice(
                        List.of(new PostIdWithScore(101, 2.5), new PostIdWithScore(100, 1.2)),
                        2L
                ));
        when(itemPostRepository.findAllWithDetailsByPostIdIn(List.of(101, 100)))
                .thenReturn(List.of(samplePost, secondPost));

        PageResponse<PostResponse> response = postService.searchPosts(
                "ví da",
                "RETURNED",
                null,
                null,
                null,
                "Tòa nhà A",
                null,
                null,
                null,
                0,
                12
        );

        ArgumentCaptor<PostSearchRequest> captor = ArgumentCaptor.forClass(PostSearchRequest.class);
        verify(itemPostRepository).searchPostIds(captor.capture());

        assertEquals(List.of(PostStatus.RESOLVED, PostStatus.CLAIMED), captor.getValue().getStatuses());
        assertEquals("Tòa nhà A", captor.getValue().getBuildingName());
        assertEquals(PostSortBy.RELEVANCE, captor.getValue().getSortBy());

        assertEquals(2, response.getItems().size());
        assertEquals(101, response.getItems().get(0).getPostId());
        assertEquals(100, response.getItems().get(1).getPostId());
    }

    @Test
    @DisplayName("GET /posts tự động chuyển sortBy=RELEVANCE sang NEWEST khi không có từ khóa")
    void shouldFallbackRelevanceToNewestWhenNoKeywordProvided() {
        when(itemPostRepository.searchPostIds(any(PostSearchRequest.class)))
                .thenReturn(new PostSearchSlice(List.of(), 0L));

        PageResponse<PostResponse> response = postService.searchPosts(
                null,
                "FOUND",
                null,
                null,
                null,
                null,
                null,
                null,
                "RELEVANCE",
                0,
                12
        );

        ArgumentCaptor<PostSearchRequest> captor = ArgumentCaptor.forClass(PostSearchRequest.class);
        verify(itemPostRepository).searchPostIds(captor.capture());
        assertEquals(PostSortBy.NEWEST, captor.getValue().getSortBy());
        assertEquals(ItemType.FOUND, captor.getValue().getItemType());
        assertEquals("NEWEST", response.getSortBy());
    }

    @Test
    @DisplayName("GET /posts hỗ trợ sắp xếp OLDEST")
    void shouldSupportOldestSortOrder() {
        when(itemPostRepository.searchPostIds(any(PostSearchRequest.class)))
                .thenReturn(new PostSearchSlice(List.of(), 0L));

        PageResponse<PostResponse> response = postService.searchPosts(
                null, null, null, null, null, null, null, null, "OLDEST", 0, 20
        );

        assertEquals("OLDEST", response.getSortBy());
        assertEquals(20, response.getSize());
    }

    @Test
    @DisplayName("GET /posts báo lỗi 400 nếu fromDate lớn hơn toDate")
    void shouldRejectInvalidDateRange() {
        AppException ex = assertThrows(AppException.class, () -> postService.searchPosts(
                null, null, null, null, null, null,
                "2026-11-15", "2026-11-10", "NEWEST", 0, 12
        ));
        assertEquals(ErrorCode.INVALID_DATE_RANGE, ex.getErrorCode());
    }

    @Test
    @DisplayName("GET /posts/:id trả về chi tiết bài đăng và ẩn hoàn toàn PhoneNumber của người đăng")
    void shouldReturnPostDetailAndHidePhoneNumber() throws Exception {
        when(itemPostRepository.findWithDetailsByPostId(100)).thenReturn(Optional.of(samplePost));

        PostResponse response = postService.getPostById(100);

        assertEquals(100, response.getPostId());
        assertEquals("Mất ví da màu đen", response.getTitle());
        assertNotNull(response.getAuthor());
        assertEquals("Nguyễn Văn A", response.getAuthor().getFullName());
        assertNull(response.getAuthor().getPhoneNumber());

        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        String json = mapper.writeValueAsString(response);
        assertFalse(json.contains("0901234567"));
        assertFalse(json.contains("phoneNumber"));
    }

    @Test
    @DisplayName("PUT /posts/:id cho phép tác giả chỉnh sửa khi bài chưa đóng")
    void shouldAllowAuthorToUpdateWhenPostIsOpen() {
        authenticateAs(authorUser);
        when(itemPostRepository.findWithDetailsByPostId(100)).thenReturn(Optional.of(samplePost));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(locationRepository.findById(1)).thenReturn(Optional.of(location));
        when(itemPostRepository.save(any(ItemPost.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdatePostRequest updateRequest = UpdatePostRequest.builder()
                .title("Mất ví da màu đen (Đã cập nhật)")
                .itemType(ItemType.LOST)
                .categoryId(1)
                .locationId(1)
                .incidentDate(LocalDateTime.of(2026, 10, 25, 11, 30))
                .description("Cập nhật thêm thông tin về giấy tờ bên trong")
                .imageUrls(List.of("https://res.cloudinary.com/demo/image/upload/sample1.jpg"))
                .build();

        PostResponse updated = postService.updatePost(100, updateRequest);

        assertEquals("Mất ví da màu đen (Đã cập nhật)", updated.getTitle());
        assertEquals(1, updated.getImageUrls().size());
    }

    @Test
    @DisplayName("PUT /posts/:id chặn người dùng không phải tác giả chỉnh sửa")
    void shouldRejectUpdateWhenNotAuthor() {
        authenticateAs(otherUser);
        when(itemPostRepository.findWithDetailsByPostId(100)).thenReturn(Optional.of(samplePost));

        UpdatePostRequest updateRequest = UpdatePostRequest.builder()
                .title("Cố tình sửa bài của người khác")
                .itemType(ItemType.LOST)
                .categoryId(1)
                .locationId(1)
                .incidentDate(LocalDateTime.now())
                .description("Mô tả")
                .build();

        AppException ex = assertThrows(AppException.class, () -> postService.updatePost(100, updateRequest));
        assertEquals(ErrorCode.NOT_POST_AUTHOR, ex.getErrorCode());
    }

    @Test
    @DisplayName("PUT /posts/:id chặn chỉnh sửa khi bài đăng đã đóng (RESOLVED / CLAIMED)")
    void shouldRejectUpdateWhenPostIsAlreadyClosed() {
        authenticateAs(authorUser);
        samplePost.setStatus(PostStatus.RESOLVED);
        when(itemPostRepository.findWithDetailsByPostId(100)).thenReturn(Optional.of(samplePost));

        UpdatePostRequest updateRequest = UpdatePostRequest.builder()
                .title("Sửa bài đã đóng")
                .itemType(ItemType.LOST)
                .categoryId(1)
                .locationId(1)
                .incidentDate(LocalDateTime.now())
                .description("Mô tả")
                .build();

        AppException ex = assertThrows(AppException.class, () -> postService.updatePost(100, updateRequest));
        assertEquals(ErrorCode.POST_ALREADY_CLOSED, ex.getErrorCode());
    }

    @Test
    @DisplayName("PATCH /posts/:id/status cho phép tác giả đổi trạng thái thành RESOLVED hoặc CLAIMED")
    void shouldAllowAuthorToMarkPostResolvedOrClaimed() {
        authenticateAs(authorUser);
        when(itemPostRepository.findWithDetailsByPostId(100)).thenReturn(Optional.of(samplePost));
        when(itemPostRepository.save(any(ItemPost.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdatePostStatusRequest statusRequest = new UpdatePostStatusRequest(PostStatus.CLAIMED);
        PostResponse response = postService.updatePostStatusByAuthor(100, statusRequest);

        assertEquals(PostStatus.CLAIMED, response.getStatus());
    }

    @Test
    @DisplayName("DELETE /posts/:id cho phép tác giả xóa bài đăng của mình")
    void shouldAllowAuthorToDeleteOwnPost() {
        authenticateAs(authorUser);
        when(itemPostRepository.findWithDetailsByPostId(100)).thenReturn(Optional.of(samplePost));

        postService.deletePost(100);

        verify(itemPostRepository).delete(samplePost);
    }

    private void authenticateAs(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
