package com.uth.capstone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO phản hồi kết quả tải lên và nén ảnh.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadResponse {

    private String originalFileName;
    private String imageUrl;
    private String storageProvider;
    private long originalSizeBytes;
    private long compressedSizeBytes;
    private String compressionRatio;
}

