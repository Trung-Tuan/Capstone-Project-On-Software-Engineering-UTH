package com.uth.capstone.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uth.capstone.dto.request.CreatePostRequest;
import com.uth.capstone.dto.request.UpdatePostRequest;
import com.uth.capstone.dto.request.UpdatePostStatusRequest;
import com.uth.capstone.dto.response.PostResponse;
import com.uth.capstone.entity.Category;
import com.uth.capstone.entity.ItemPost;
import com.uth.capstone.entity.Location;
import com.uth.capstone.entity.User;
import com.uth.capstone.enums.ItemType;
import com.uth.capstone.enums.PostStatus;
import com.uth.capstone.enums.Role;
import com.uth.capstone.enums.UserStatus;
import com.uth.capstone.exception.AppException;
import com.uth.capstone.exception.ErrorCode;
import com.uth.capstone.repository.CategoryRepository;
import com.uth.capstone.repository.ItemPostRepository;
import com.uth.capstone.repository.LocationRepository;
import com.uth.capstone.repository.UserRepository;
import com.uth.capstone.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

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
    private UserRepository userRepository;
    private PostService postService;

    private User authorUser;
    private User otherUser;
    private User adminUser;
    private User lockedUser;
    private Category category;
    private Location location;
    private ItemPost samplePost;

    @BeforeEach
    void setUp() {
        itemPostRepository = Mockito.mock(ItemPostRepository.class);
        categoryRepository = Mockito.mock(CategoryRepository.class);
        locationRepository = Mockito.mock(LocationRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        postService = new PostService(itemPostRepository, categoryRepository, locationRepository, userRepository);
        ReflectionTestUtils.setField(postService, "autoApprove", false);

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

        adminUser = User.builder()
                .userId(3)
                .email("admin@ut.edu.vn")
                .fullName("Quản Trị Viên")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        lockedUser = User.builder()
                .userId(4)
                .email("locked@ut.edu.vn")
                .fullName("Tài Khoản Bị Khóa")
                .role(Role.STUDENT)
                .status(UserStatus.LOCKED)
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
                .createdAt(LocalDateTime.now())
                .images(new ArrayList<>())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /posts tạo mới bài đăng với trạng thái PENDING khi tắt auto-approve")
    void shouldCreatePostWithPendingStatusWhenAutoApproveDisabled() {
        authenticateAs(authorUser);
        when(userRepository.findById(authorUser.getUserId())).thenReturn(Optional.of(authorUser));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(locationRepository.findById(1)).thenReturn(Optional.of(location));
        when(itemPostRepository.save(any(ItemPost.class))).thenAnswer(inv -> {
            ItemPost saved = inv.getArgument(0);
            saved.setPostId(201);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        CreatePostRequest createRequest = CreatePostRequest.builder()
                .title("  Nhặt được tai nghe AirPods Pro  ")
                .itemType(ItemType.FOUND)
                .categoryId(1)
                .locationId(1)
                .incidentDate(LocalDateTime.of(2026, 11, 11, 9, 30))
                .description("  Nhặt được tại bàn học cuối phòng A201  ")
                .imageUrls(List.of(
                        "https://res.cloudinary.com/demo/image/upload/airpods1.jpg",
                        "https://res.cloudinary.com/demo/image/upload/airpods2.jpg"
                ))
                .build();

        PostResponse created = postService.createPost(createRequest);

        assertEquals(201, created.getPostId());
        assertEquals("Nhặt được tai nghe AirPods Pro", created.getTitle());
        assertEquals("Nhặt được tại bàn học cuối phòng A201", created.getDescription());
        assertEquals(ItemType.FOUND, created.getItemType());
        assertEquals(PostStatus.PENDING, created.getStatus());
        assertEquals(2, created.getImageUrls().size());
        assertNull(created.getAuthor().getPhoneNumber());
    }

    @Test
    @DisplayName("POST /posts tạo mới bài đăng với trạng thái APPROVED khi bật auto-approve")
    void shouldCreatePostWithApprovedStatusWhenAutoApproveEnabled() {
        authenticateAs(authorUser);
        ReflectionTestUtils.setField(postService, "autoApprove", true);
        when(userRepository.findById(authorUser.getUserId())).thenReturn(Optional.of(authorUser));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(locationRepository.findById(1)).thenReturn(Optional.of(location));
        when(itemPostRepository.save(any(ItemPost.class))).thenAnswer(inv -> inv.getArgument(0));

        CreatePostRequest createRequest = CreatePostRequest.builder()
                .title("Mất chìa khóa xe máy")
                .itemType(ItemType.LOST)
                .categoryId(1)
                .locationId(1)
                .incidentDate(LocalDateTime.of(2026, 11, 11, 8, 0))
                .description("Rơi chìa khóa màu đen có móc khóa xanh")
                .build();

        PostResponse created = postService.createPost(createRequest);

        assertEquals(PostStatus.APPROVED, created.getStatus());
        assertTrue(created.getImageUrls().isEmpty());
    }

    @Test
    @DisplayName("POST /posts tự động duyệt APPROVED nếu người đăng là Admin / Security / SAC")
    void shouldAutoApproveWhenAuthorIsAdminSecurityOrSac() {
        authenticateAs(adminUser);
        when(userRepository.findById(adminUser.getUserId())).thenReturn(Optional.of(adminUser));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(locationRepository.findById(1)).thenReturn(Optional.of(location));
        when(itemPostRepository.save(any(ItemPost.class))).thenAnswer(inv -> inv.getArgument(0));

        CreatePostRequest createRequest = CreatePostRequest.builder()
                .title("Lưu kho: Thẻ sinh viên nhặt được tại sảnh A")
                .itemType(ItemType.FOUND)
                .categoryId(1)
                .locationId(1)
                .incidentDate(LocalDateTime.of(2026, 11, 11, 10, 0))
                .description("Đang lưu giữ tại Phòng CTSV")
                .build();

        PostResponse created = postService.createPost(createRequest);

        assertEquals(PostStatus.APPROVED, created.getStatus());
    }

    @Test
    @DisplayName("POST /posts ném lỗi CATEGORY_NOT_FOUND khi danh mục không tồn tại")
    void shouldRejectCreatePostWhenCategoryNotFound() {
        authenticateAs(authorUser);
        when(userRepository.findById(authorUser.getUserId())).thenReturn(Optional.of(authorUser));
        when(categoryRepository.findById(999)).thenReturn(Optional.empty());

        CreatePostRequest createRequest = CreatePostRequest.builder()
                .title("Mất đồ")
                .itemType(ItemType.LOST)
                .categoryId(999)
                .locationId(1)
                .incidentDate(LocalDateTime.now())
                .description("Mô tả")
                .build();

        AppException ex = assertThrows(AppException.class, () -> postService.createPost(createRequest));
        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("POST /posts ném lỗi TOO_MANY_IMAGES khi đính kèm quá 3 ảnh")
    void shouldRejectCreatePostWhenMoreThanThreeImages() {
        authenticateAs(authorUser);
        when(userRepository.findById(authorUser.getUserId())).thenReturn(Optional.of(authorUser));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(locationRepository.findById(1)).thenReturn(Optional.of(location));

        CreatePostRequest createRequest = CreatePostRequest.builder()
                .title("Mất balo laptop")
                .itemType(ItemType.LOST)
                .categoryId(1)
                .locationId(1)
                .incidentDate(LocalDateTime.now())
                .description("Mô tả chi tiết")
                .imageUrls(List.of("1.jpg", "2.jpg", "3.jpg", "4.jpg"))
                .build();

        AppException ex = assertThrows(AppException.class, () -> postService.createPost(createRequest));
        assertEquals(ErrorCode.TOO_MANY_IMAGES, ex.getErrorCode());
    }

    @Test
    @DisplayName("POST /posts chặn tài khoản bị khóa (Status = LOCKED)")
    void shouldRejectCreatePostWhenUserIsLocked() {
        authenticateAs(lockedUser);
        when(userRepository.findById(lockedUser.getUserId())).thenReturn(Optional.of(lockedUser));

        CreatePostRequest createRequest = CreatePostRequest.builder()
                .title("Mất đồ")
                .itemType(ItemType.LOST)
                .categoryId(1)
                .locationId(1)
                .incidentDate(LocalDateTime.now())
                .description("Mô tả")
                .build();

        AppException ex = assertThrows(AppException.class, () -> postService.createPost(createRequest));
        assertEquals(ErrorCode.USER_LOCKED, ex.getErrorCode());
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
