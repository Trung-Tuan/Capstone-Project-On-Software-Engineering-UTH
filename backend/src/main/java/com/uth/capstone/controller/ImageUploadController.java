package com.uth.capstone.controller;

import com.uth.capstone.dto.response.ApiResponse;
import com.uth.capstone.dto.response.ImageUploadResponse;
import com.uth.capstone.service.ImageStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controller cung cấp API tải ảnh đính kèm bài đăng (tối đa 3 ảnh/bài, 5MB/ảnh, tự động nén trên server).
 */
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Tag(
        name = "Image Storage",
        description = "API Tải lên, kiểm tra giới hạn (tối đa 3 ảnh/bài, 5MB/ảnh) và tự động nén ảnh (Cloudinary / Local Storage)"
)
public class ImageUploadController {

    private final ImageStorageService imageStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Tải lên và tự động nén danh sách ảnh bài đăng (Tối đa 3 ảnh, <= 5MB/ảnh)",
            description = "Middleware và Service tự động kiểm tra số lượng tối đa 3 ảnh/bài đăng, dung lượng tối đa 5MB/ảnh, nén giảm dung lượng trên server trước khi lưu lên Cloudinary hoặc Local Storage."
    )
    public ResponseEntity<ApiResponse<List<ImageUploadResponse>>> uploadPostImages(
            @RequestPart("files") List<MultipartFile> files
    ) {
        List<ImageUploadResponse> uploadedImages = imageStorageService.uploadPostImagesWithDetails(files);
        return ResponseEntity.ok(ApiResponse.success("Tải lên và nén ảnh thành công", uploadedImages));
    }
}

