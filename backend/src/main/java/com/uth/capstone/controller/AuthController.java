package com.uth.capstone.controller;

import com.uth.capstone.dto.request.GoogleLoginRequest;
import com.uth.capstone.dto.response.ApiResponse;
import com.uth.capstone.dto.response.AuthResponse;
import com.uth.capstone.dto.response.UserResponse;
import com.uth.capstone.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller cung cấp API xác thực Google OAuth2 và tra cứu phiên đăng nhập hiện tại.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API Xác thực Google OAuth2 (@ut.edu.vn) & Phân quyền RBAC")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/google")
    @Operation(
            summary = "Đăng nhập bằng Google Identity Services (OAuth2)",
            description = "Xác thực Google ID Token, kiểm tra ràng buộc đuôi email trường (@ut.edu.vn), chặn tài khoản bị khóa và cấp phát JWT Access Token."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithGoogle(
            @Valid @RequestBody GoogleLoginRequest request
    ) {
        AuthResponse authResponse = authService.authenticateWithGoogle(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", authResponse));
    }

    @GetMapping("/me")
    @Operation(
            summary = "Lấy thông tin người dùng đang đăng nhập",
            description = "Yêu cầu gửi kèm JWT Bearer Token trong Header Authorization."
    )
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UserResponse currentUser = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(currentUser));
    }
}
