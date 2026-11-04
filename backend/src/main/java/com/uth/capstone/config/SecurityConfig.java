package com.uth.capstone.config;

import com.uth.capstone.security.CustomAccessDeniedHandler;
import com.uth.capstone.security.CustomAuthenticationEntryPoint;
import com.uth.capstone.security.JwtAuthenticationFilter;
import com.uth.capstone.security.SchoolEmailEnforcementFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SchoolEmailEnforcementFilter schoolEmailEnforcementFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // Swagger & OpenAPI Documentation
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Public Auth Endpoint (Google OAuth2 Login)
                        .requestMatchers("/api/auth/google", "/auth/google").permitAll()
                        // Public GET Endpoints (Tra cứu, tìm kiếm bài đăng, danh mục, địa điểm, xem ảnh Local Storage)
                        .requestMatchers(HttpMethod.GET,
                                "/api/posts/**", "/posts/**",
                                "/api/categories/**", "/categories/**",
                                "/api/locations/**", "/locations/**",
                                "/uploads/**"
                        ).permitAll()
                        // Cổng Quản trị & Kiểm duyệt (Admin / Security / SAC)
                        .requestMatchers("/api/admin/**", "/admin/**")
                        .hasAnyRole("ADMIN", "SECURITY", "SAC")
                        // Các thao tác Đăng bài, Sửa/Xóa bài, Gửi/Xử lý Claim, Upload ảnh (Student / Staff / Admin / Security / SAC)
                        .requestMatchers(
                                "/api/posts/**", "/posts/**",
                                "/api/claims/**", "/claims/**",
                                "/api/images/**", "/images/**",
                                "/api/auth/me", "/auth/me",
                                "/api/profile/**", "/profile/**"
                        ).hasAnyRole("STUDENT", "STAFF", "ADMIN", "SECURITY", "SAC")
                        // Tất cả request còn lại yêu cầu phải xác thực
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(schoolEmailEnforcementFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
