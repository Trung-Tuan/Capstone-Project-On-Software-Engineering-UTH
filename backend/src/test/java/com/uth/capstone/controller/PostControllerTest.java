package com.uth.capstone.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uth.capstone.config.SecurityConfig;
import com.uth.capstone.dto.request.CreatePostRequest;
import com.uth.capstone.entity.Category;
import com.uth.capstone.entity.ItemPost;
import com.uth.capstone.entity.Location;
import com.uth.capstone.entity.User;
import com.uth.capstone.enums.ItemType;
import com.uth.capstone.enums.Role;
import com.uth.capstone.enums.UserStatus;
import com.uth.capstone.exception.ErrorCode;
import com.uth.capstone.exception.GlobalExceptionHandler;
import com.uth.capstone.repository.CategoryRepository;
import com.uth.capstone.repository.ItemPostRepository;
import com.uth.capstone.repository.LocationRepository;
import com.uth.capstone.repository.UserRepository;
import com.uth.capstone.security.*;
import com.uth.capstone.service.PostService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Kiểm thử tầng Controller + Security + Validation cho API POST /posts và POST /api/posts.
 */
@WebMvcTest(controllers = PostController.class)
@Import({
        SecurityConfig.class,
        JwtTokenProvider.class,
        SchoolEmailValidator.class,
        JwtAuthenticationFilter.class,
        SchoolEmailEnforcementFilter.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class,
        CustomUserDetailsService.class,
        GlobalExceptionHandler.class,
        PostService.class
})
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostService postService;

    @MockBean
    private ItemPostRepository itemPostRepository;

    @MockBean
    private CategoryRepository categoryRepository;

    @MockBean
    private LocationRepository locationRepository;

    @MockBean
    private UserRepository userRepository;

    private User studentUser;
    private User outsiderUser;
    private User adminUser;
    private Category category;
    private Location location;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(postService, "autoApprove", false);

        studentUser = User.builder()
                .userId(1)
                .email("2251120001@ut.edu.vn")
                .fullName("Việt Quốc")
                .studentId("2251120001")
                .phoneNumber("0901234567")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .build();

        outsiderUser = User.builder()
                .userId(2)
                .email("outsider@gmail.com")
                .fullName("Người Ngoài Trường")
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

        category = Category.builder()
                .categoryId(3)
                .categoryName("Ví")
                .icon("fa-solid fa-wallet")
                .build();

        location = Location.builder()
                .locationId(4)
                .buildingName("Tòa A")
                .floor("Tầng 2")
                .roomZone("Phòng A201")
                .build();
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(postService, "autoApprove", false);
    }

    @Test
    @DisplayName("1. Chặn HTTP 401 khi gọi POST /posts mà chưa đăng nhập")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        CreatePostRequest request = buildValidRequest();

        mockMvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()));
    }

    @Test
    @DisplayName("2. Chặn HTTP 403 khi tài khoản không dùng email trường (@ut.edu.vn)")
    void shouldReturn403WhenNonSchoolEmail() throws Exception {
        when(userRepository.findByEmail(outsiderUser.getEmail())).thenReturn(Optional.of(outsiderUser));
        String token = jwtTokenProvider.generateToken(outsiderUser);

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_SCHOOL_EMAIL.getCode()));
    }

    @Test
    @DisplayName("3. Trả về HTTP 400 khi thiếu dữ liệu bắt buộc hoặc vượt quá 3 ảnh")
    void shouldReturn400WhenValidationFails() throws Exception {
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));
        String token = jwtTokenProvider.generateToken(studentUser);

        CreatePostRequest invalidRequest = CreatePostRequest.builder()
                .title("")
                .itemType(null)
                .categoryId(3)
                .locationId(4)
                .incidentDate(LocalDateTime.of(2026, 11, 11, 9, 0))
                .description("Mô tả")
                .imageUrls(List.of("1.jpg", "2.jpg", "3.jpg", "4.jpg"))
                .build();

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.getCode()));
    }

    @Test
    @DisplayName("4. Tạo bài đăng thành công (HTTP 201) qua POST /posts ở trạng thái PENDING và ẩn PhoneNumber")
    void shouldCreatePostSuccessfullyAsPending() throws Exception {
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));
        when(userRepository.findById(studentUser.getUserId())).thenReturn(Optional.of(studentUser));
        when(categoryRepository.findById(3)).thenReturn(Optional.of(category));
        when(locationRepository.findById(4)).thenReturn(Optional.of(location));
        when(itemPostRepository.save(any(ItemPost.class))).thenAnswer(inv -> {
            ItemPost post = inv.getArgument(0);
            post.setPostId(101);
            post.setCreatedAt(LocalDateTime.now());
            return post;
        });

        String token = jwtTokenProvider.generateToken(studentUser);

        mockMvc.perform(post("/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.postId").value(101))
                .andExpect(jsonPath("$.result.status").value("PENDING"))
                .andExpect(jsonPath("$.result.imageUrls.length()").value(2))
                .andExpect(content().string(not(containsString("0901234567"))))
                .andExpect(content().string(not(containsString("phoneNumber"))));
    }

    @Test
    @DisplayName("5. Tạo bài đăng thành công (HTTP 201) ở trạng thái APPROVED khi bật auto-approve hoặc Admin đăng")
    void shouldCreatePostAsApprovedForAdmin() throws Exception {
        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(adminUser.getUserId())).thenReturn(Optional.of(adminUser));
        when(categoryRepository.findById(3)).thenReturn(Optional.of(category));
        when(locationRepository.findById(4)).thenReturn(Optional.of(location));
        when(itemPostRepository.save(any(ItemPost.class))).thenAnswer(inv -> {
            ItemPost post = inv.getArgument(0);
            post.setPostId(102);
            post.setCreatedAt(LocalDateTime.now());
            return post;
        });

        String adminToken = jwtTokenProvider.generateToken(adminUser);

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.postId").value(102))
                .andExpect(jsonPath("$.result.status").value("APPROVED"));
    }

    private CreatePostRequest buildValidRequest() {
        return CreatePostRequest.builder()
                .title("Nhặt được ví da nâu tại phòng A201")
                .itemType(ItemType.FOUND)
                .categoryId(3)
                .locationId(4)
                .incidentDate(LocalDateTime.of(2026, 11, 11, 9, 15))
                .description("Ví da màu nâu có 1 thẻ giữ xe, chưa rõ chủ nhân")
                .imageUrls(List.of(
                        "https://res.cloudinary.com/demo/image/upload/vi1.jpg",
                        "https://res.cloudinary.com/demo/image/upload/vi2.jpg"
                ))
                .build();
    }
}
