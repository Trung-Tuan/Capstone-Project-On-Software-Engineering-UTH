package com.uth.capstone.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.uth.capstone.config.CloudinaryConfig;
import com.uth.capstone.config.ImageUploadValidationInterceptor;
import com.uth.capstone.dto.response.ImageUploadResponse;
import com.uth.capstone.exception.AppException;
import com.uth.capstone.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dịch vụ xử lý kiểm tra giới hạn, tự động nén ảnh trên server
 * và lưu trữ linh hoạt lên Cloudinary hoặc Local Storage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageStorageService {

    private final Cloudinary cloudinary;
    private final CloudinaryConfig cloudinaryConfig;
    private final ImageUploadValidationInterceptor validationInterceptor;

    @Value("${app.storage.provider:cloudinary}")
    private String storageProvider;

    @Value("${app.storage.local-dir:uploads}")
    private String localStorageDir;

    @Value("${app.storage.max-files-per-post:3}")
    private int maxFilesPerPost;

    @Value("${app.storage.max-file-size-bytes:5242880}")
    private long maxFileSizeBytes;

    @Value("${app.storage.compression-quality:0.75}")
    private double compressionQuality;

    @Value("${app.storage.max-width:1280}")
    private int maxWidth;

    @Value("${app.storage.max-height:1280}")
    private int maxHeight;

    @Value("${app.cloudinary.folder:uth_lost_and_found}")
    private String cloudinaryFolder;

    /**
     * Kiểm tra giới hạn (tối đa 3 ảnh/bài đăng, tối đa 5MB/ảnh),
     * tự động nén và tải danh sách ảnh lên hệ thống lưu trữ.
     *
     * @param files Danh sách file ảnh đính kèm bài đăng
     * @return Danh sách URL của các ảnh đã lưu
     */
    public List<String> uploadPostImages(List<MultipartFile> files) {
        return uploadPostImagesWithDetails(files).stream()
                .map(ImageUploadResponse::getImageUrl)
                .toList();
    }

    /**
     * Kiểm tra giới hạn, tự động nén và tải danh sách ảnh (trả về kèm thông số nén chi tiết).
     */
    public List<ImageUploadResponse> uploadPostImagesWithDetails(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        List<MultipartFile> validFiles = files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();

        if (validFiles.isEmpty()) {
            return Collections.emptyList();
        }

        // Kiểm tra giới hạn số lượng tối đa 3 ảnh / bài đăng
        if (validFiles.size() > maxFilesPerPost) {
            throw new AppException(ErrorCode.TOO_MANY_IMAGES);
        }

        List<ImageUploadResponse> responses = new ArrayList<>();
        for (MultipartFile file : validFiles) {
            responses.add(uploadSingleImageWithDetails(file));
        }
        return responses;
    }

    /**
     * Kiểm tra dung lượng (tối đa 5MB), tự động nén và tải lên 1 ảnh đơn lẻ
     * (ví dụ: ảnh minh chứng cho ClaimRequest).
     */
    public String uploadSingleImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        return uploadSingleImageWithDetails(file).getImageUrl();
    }

    /**
     * Thực hiện kiểm tra, nén và lưu 1 file ảnh kèm thông tin chi tiết.
     */
    public ImageUploadResponse uploadSingleImageWithDetails(MultipartFile file) {
        validateSingleFile(file);

        try {
            long originalSize = file.getSize();
            byte[] compressedBytes = compressImage(file);
            long compressedSize = compressedBytes.length;

            boolean useCloudinary = "cloudinary".equalsIgnoreCase(storageProvider)
                    && cloudinaryConfig.isCloudinaryConfigured();

            String imageUrl;
            String activeProvider;
            if (useCloudinary) {
                imageUrl = uploadToCloudinary(compressedBytes);
                activeProvider = "CLOUDINARY";
            } else {
                imageUrl = saveToLocalStorage(compressedBytes);
                activeProvider = "LOCAL";
            }

            double savedPercent = originalSize > 0
                    ? Math.max(0.0, (1.0 - ((double) compressedSize / originalSize)) * 100.0)
                    : 0.0;

            String compressionRatioText = String.format("-%.1f%%", savedPercent);
            log.info("Đã xử lý & lưu ảnh [{}] qua {}: {} bytes -> {} bytes ({})",
                    file.getOriginalFilename(), activeProvider, originalSize, compressedSize, compressionRatioText);

            return ImageUploadResponse.builder()
                    .originalFileName(file.getOriginalFilename())
                    .imageUrl(imageUrl)
                    .storageProvider(activeProvider)
                    .originalSizeBytes(originalSize)
                    .compressedSizeBytes(compressedSize)
                    .compressionRatio(compressionRatioText)
                    .build();
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi nén hoặc tải ảnh lên hệ thống lưu trữ: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    private void validateSingleFile(MultipartFile file) {
        if (file.getSize() > maxFileSizeBytes) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }
        if (!validationInterceptor.isValidImageFile(file)) {
            throw new AppException(ErrorCode.INVALID_IMAGE_FORMAT);
        }
    }

    /**
     * Tự động thu phóng (nếu vượt quá kích thước tối đa) và nén chất lượng ảnh trên Server.
     */
    public byte[] compressImage(MultipartFile file) throws IOException {
        BufferedImage inputImage = ImageIO.read(file.getInputStream());
        if (inputImage == null) {
            // Trường hợp định dạng WebP đặc thù không có ImageReader mặc định trên JVM chuẩn
            String contentType = file.getContentType();
            if (contentType != null && contentType.equalsIgnoreCase("image/webp")) {
                return file.getBytes();
            }
            throw new AppException(ErrorCode.INVALID_IMAGE_FORMAT);
        }

        int width = inputImage.getWidth();
        int height = inputImage.getHeight();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Thumbnails.Builder<BufferedImage> builder = Thumbnails.of(inputImage);

        if (width > maxWidth || height > maxHeight) {
            builder.size(maxWidth, maxHeight);
        } else {
            builder.scale(1.0);
        }

        builder.imageType(BufferedImage.TYPE_INT_RGB)
                .outputFormat("jpg")
                .outputQuality(compressionQuality)
                .toOutputStream(outputStream);

        byte[] compressedBytes = outputStream.toByteArray();
        byte[] originalBytes = file.getBytes();

        // Nếu ảnh gốc vốn đã rất nhỏ và nhỏ hơn bản sau khi encode lại, giữ nguyên bản gốc
        return compressedBytes.length < originalBytes.length ? compressedBytes : originalBytes;
    }

    private String uploadToCloudinary(byte[] imageBytes) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(
                imageBytes,
                ObjectUtils.asMap(
                        "folder", cloudinaryFolder,
                        "resource_type", "image"
                )
        );
        Object secureUrl = uploadResult.get("secure_url");
        if (secureUrl == null) {
            throw new AppException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
        return secureUrl.toString();
    }

    private String saveToLocalStorage(byte[] imageBytes) throws IOException {
        Path uploadDir = Paths.get(localStorageDir).toAbsolutePath().normalize();
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        String fileName = UUID.randomUUID() + ".jpg";
        Path targetPath = uploadDir.resolve(fileName);
        Files.write(targetPath, imageBytes);

        return "/uploads/" + fileName;
    }
}
