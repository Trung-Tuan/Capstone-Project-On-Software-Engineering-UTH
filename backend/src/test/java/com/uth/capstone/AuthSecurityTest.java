package com.uth.capstone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uth.capstone.config.SecurityConfig;
import com.uth.capstone.controller.AuthController;
import com.uth.capstone.dto.response.ApiResponse;
import com.uth.capstone.entity.User;
import com.uth.capstone.enums.Role;
import com.uth.capstone.enums.UserStatus;
import com.uth.capstone.exception.ErrorCode;
import com.uth.capstone.exception.GlobalExceptionHandler;
import com.uth.capstone.repository.UserRepository;
import com.uth.capstone.security.*;
import com.uth.capstone.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử tự động cho luồng Xác thực Google OAuth2, Ràng buộc Email trường (@ut.edu.vn),
 * Chặn tài khoản bị khóa (LOCKED) và Phân quyền RBAC (Student/Staff vs Admin/Security/SAC).
 */
@WebMvcTest(controllers = {AuthController.class, AuthSecurityTest.DummyRbacController.class})
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
        AuthService.class,
        AuthSecurityTest.DummyRbacController.class
})
class AuthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private SchoolEmailValidator schoolEmailValidator;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    private User activeStudent;
    private User lockedStudent;
    private User nonSchoolEmailUser;
    private User activeAdmin;
    private User activeSecuritySac;

    @RestController
    static class DummyRbacController {
        @PostMapping("/api/posts")
        public ResponseEntity<ApiResponse<String>> createPost() {
            return ResponseEntity.ok(ApiResponse.success("Tạo bài đăng thành công"));
        }

        @PostMapping("/api/claims")
        public ResponseEntity<ApiResponse<String>> createClaim() {
            return ResponseEntity.ok(ApiResponse.success("Gửi yêu cầu nhận đồ thành công"));
        }

        @GetMapping("/api/admin/dashboard")
        public ResponseEntity<ApiResponse<String>> adminDashboard() {
            return ResponseEntity.ok(ApiResponse.success("Truy cập cổng Admin thành công"));
        }
    }

    @BeforeEach
    void setUp() {
        activeStudent = User.builder()
                .userId(1)
                .email("2251120001@ut.edu.vn")
                .fullName("Việt Quốc")
                .studentId("2251120001")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .build();

        lockedStudent = User.builder()
                .userId(2)
                .email("locked.student@ut.edu.vn")
                .fullName("Sinh Viên Bị Khóa")
                .studentId("2251120002")
                .role(Role.STUDENT)
                .status(UserStatus.LOCKED)
                .build();

        nonSchoolEmailUser = User.builder()
                .userId(3)
                .email("outsider@gmail.com")
                .fullName("Người Ngoài Trường")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .build();

        activeAdmin = User.builder()
                .userId(4)
                .email("admin@ut.edu.vn")
                .fullName("Quản Trị Viên")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        activeSecuritySac = User.builder()
                .userId(5)
                .email("sac@ut.edu.vn")
                .fullName("Cán Bộ CTSV")
                .role(Role.SAC)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("1. Kiểm tra ràng buộc đuôi email trường (@ut.edu.vn / @uth.edu.vn)")
    void testSchoolEmailValidation() {
        assertTrue(schoolEmailValidator.isValidSchoolEmail("2251120001@ut.edu.vn"));
        assertTrue(schoolEmailValidator.isValidSchoolEmail("giangvien@uth.edu.vn"));
        assertTrue(schoolEmailValidator.isValidSchoolEmail("sv@sv.ut.edu.vn"));
        assertFalse(schoolEmailValidator.isValidSchoolEmail("sinhvien@gmail.com"));
        assertFalse(schoolEmailValidator.isValidSchoolEmail("fake@ut.edu.vn.hacker.com"));
    }

    @Test
    @DisplayName("2. Từ chối Google ID Token giả mạo khi gọi POST /api/auth/google")
    void testGoogleLoginWithInvalidTokenShouldReturn401() throws Exception {
        String body = "{\"idToken\": \"invalid.google.token\"}";

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_GOOGLE_TOKEN.getCode()));
    }

    @Test
    @DisplayName("3. Chặn truy cập đối với tài khoản bị khóa (Status = LOCKED)")
    void testLockedUserShouldBeBlockedByMiddleware() throws Exception {
        when(userRepository.findByEmail(lockedStudent.getEmail())).thenReturn(Optional.of(lockedStudent));
        String token = jwtTokenProvider.generateToken(lockedStudent);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_LOCKED.getCode()));
    }

    @Test
    @DisplayName("4. Middleware chặn đăng bài & gửi claim nếu không dùng email trường (@ut.edu.vn)")
    void testMiddlewareBlocksNonSchoolEmailWhenCreatingPostOrClaim() throws Exception {
        when(userRepository.findByEmail(nonSchoolEmailUser.getEmail())).thenReturn(Optional.of(nonSchoolEmailUser));
        String outsiderToken = jwtTokenProvider.generateToken(nonSchoolEmailUser);

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_SCHOOL_EMAIL.getCode()));

        mockMvc.perform(post("/api/claims")
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_SCHOOL_EMAIL.getCode()));
    }

    @Test
    @DisplayName("5. Sinh viên có email trường (@ut.edu.vn) được phép đăng bài & gửi claim, nhưng bị chặn vào cổng Admin")
    void testStudentRbacPermissions() throws Exception {
        when(userRepository.findByEmail(activeStudent.getEmail())).thenReturn(Optional.of(activeStudent));
        when(userRepository.findById(activeStudent.getUserId())).thenReturn(Optional.of(activeStudent));
        String studentToken = jwtTokenProvider.generateToken(activeStudent);

        // Được phép gọi /api/auth/me, /api/posts, /api/claims
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.email").value("2251120001@ut.edu.vn"));

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Bị chặn khi cố tình truy cập API dành riêng cho Admin/Security/SAC
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("6. Phân quyền RBAC: Admin và Security/SAC được phép truy cập cổng quản trị /api/admin/**")
    void testAdminAndSacRbacPermissions() throws Exception {
        when(userRepository.findByEmail(activeAdmin.getEmail())).thenReturn(Optional.of(activeAdmin));
        when(userRepository.findByEmail(activeSecuritySac.getEmail())).thenReturn(Optional.of(activeSecuritySac));

        String adminToken = jwtTokenProvider.generateToken(activeAdmin);
        String sacToken = jwtTokenProvider.generateToken(activeSecuritySac);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + sacToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
