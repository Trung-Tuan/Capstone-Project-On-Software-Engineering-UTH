package com.uth.capstone.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uth.capstone.dto.response.ApiResponse;
import com.uth.capstone.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Middleware kiểm tra ràng buộc đuôi email trường (ví dụ: @ut.edu.vn)
 * khi người dùng thực hiện đăng bài hoặc gửi yêu cầu nhận đồ (Mục 2.4 SRS & WBS 1.2).
 */
@Component
@RequiredArgsConstructor
public class SchoolEmailEnforcementFilter extends OncePerRequestFilter {

    private final SchoolEmailValidator schoolEmailValidator;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (requiresSchoolEmailCheck(request)) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
                if (!schoolEmailValidator.isValidSchoolEmail(userDetails.getEmail())) {
                    response.setStatus(ErrorCode.INVALID_SCHOOL_EMAIL.getStatusCode().value());
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    ApiResponse<Object> apiResponse = ApiResponse.error(
                            ErrorCode.INVALID_SCHOOL_EMAIL.getCode(),
                            ErrorCode.INVALID_SCHOOL_EMAIL.getMessage()
                    );
                    response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresSchoolEmailCheck(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        // Kiểm tra khi người dùng thực hiện tạo/cập nhật bài đăng hoặc gửi yêu cầu nhận đồ (Claim)
        boolean isWriteAction = "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method);

        boolean isPostOrClaimEndpoint = uri.startsWith("/api/posts")
                || uri.startsWith("/posts")
                || uri.startsWith("/api/claims")
                || uri.startsWith("/claims");

        return isWriteAction && isPostOrClaimEndpoint;
    }
}
