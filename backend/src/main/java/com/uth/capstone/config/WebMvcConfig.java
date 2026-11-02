package com.uth.capstone.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Cấu hình Spring Web MVC:
 * - Đăng ký Middleware (Interceptor) kiểm tra giới hạn số lượng và dung lượng ảnh tải lên.
 * - Ánh xạ đường dẫn tĩnh /uploads/** để truy cập ảnh khi lưu ở chế độ Local Storage.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ImageUploadValidationInterceptor imageUploadValidationInterceptor;

    @Value("${app.storage.local-dir:uploads}")
    private String localStorageDir;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(imageUploadValidationInterceptor)
                .addPathPatterns(
                        "/api/**",
                        "/posts/**",
                        "/claims/**",
                        "/images/**"
                );
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(localStorageDir).toAbsolutePath().normalize();
        String resourceLocation = uploadPath.toUri().toString();
        if (!resourceLocation.endsWith("/")) {
            resourceLocation += "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation);
    }
}

