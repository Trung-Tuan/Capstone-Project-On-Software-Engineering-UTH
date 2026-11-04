package com.uth.capstone.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uth.capstone.dto.response.ApiResponse;
import com.uth.capstone.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Middleware (Interceptor) kiểm tra ràng buộc file ảnh tải lên:
 * - Tối đa 3 ảnh / bài đăng (max-files-per-post = 3)
 * - Dung lượng tối đa 5MB / ảnh (max-file-size-bytes = 5MB)
 * - Định dạng ảnh hợp lệ (JPG, JPEG, PNG, WEBP)
 */
@Component
@RequiredArgsConstructor
public class ImageUploadValidationInterceptor implements HandlerInterceptor {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg",
            "jpeg",
            "png",
            "webp"
    );

    private final ObjectMapper objectMapper;

    @Value("${app.storage.max-files-per-post:3}")
    private int maxFilesPerPost;

    @Value("${app.storage.max-file-size-bytes:5242880}")
    private long maxFileSizeBytes;

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) throws IOException {

        if (!(request instanceof MultipartHttpServletRequest multipartRequest)) {
            return true;
        }

        List<MultipartFile> uploadedFiles = extractUploadedFiles(multipartRequest);
        if (uploadedFiles.isEmpty()) {
            return true;
        }

        // 1. Kiểm tra giới hạn số lượng tối đa 3 ảnh / bài đăng
        if (uploadedFiles.size() > maxFilesPerPost) {
            writeErrorResponse(response, ErrorCode.TOO_MANY_IMAGES);
            return false;
        }

        // 2. Kiểm tra dung lượng tối đa 5MB / ảnh và định dạng ảnh hợp lệ
        for (MultipartFile file : uploadedFiles) {
            if (file.getSize() > maxFileSizeBytes) {
                writeErrorResponse(response, ErrorCode.FILE_TOO_LARGE);
                return false;
            }

            if (!isValidImageFile(file)) {
                writeErrorResponse(response, ErrorCode.INVALID_IMAGE_FORMAT);
                return false;
            }
        }

        return true;
    }

    private List<MultipartFile> extractUploadedFiles(MultipartHttpServletRequest multipartRequest) {
        List<MultipartFile> files = new ArrayList<>();
        multipartRequest.getMultiFileMap().values().forEach(fileList -> {
            for (MultipartFile file : fileList) {
                if (file != null && !file.isEmpty() && !isJsonPart(file)) {
                    files.add(file);
                }
            }
        });
        return files;
    }

    private boolean isJsonPart(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith(MediaType.APPLICATION_JSON_VALUE);
    }

    public boolean isValidImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            return false;
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return false;
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(extension);
    }

    private void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatusCode().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Object> apiResponse = ApiResponse.error(errorCode.getCode(), errorCode.getMessage());
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}

