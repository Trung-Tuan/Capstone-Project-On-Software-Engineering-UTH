package com.uth.capstone.service;

import com.cloudinary.Cloudinary;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uth.capstone.config.CloudinaryConfig;
import com.uth.capstone.config.ImageUploadValidationInterceptor;
import com.uth.capstone.dto.response.ImageUploadResponse;
import com.uth.capstone.exception.AppException;
import com.uth.capstone.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ImageStorageServiceTest {

    @TempDir
    Path tempUploadDir;

    private ImageUploadValidationInterceptor interceptor;
    private ImageStorageService imageStorageService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        interceptor = new ImageUploadValidationInterceptor(objectMapper);
        ReflectionTestUtils.setField(interceptor, "maxFilesPerPost", 3);
        ReflectionTestUtils.setField(interceptor, "maxFileSizeBytes", 5L * 1024 * 1024);

        Cloudinary cloudinary = Mockito.mock(Cloudinary.class);
        CloudinaryConfig cloudinaryConfig = new CloudinaryConfig();

        imageStorageService = new ImageStorageService(cloudinary, cloudinaryConfig, interceptor);
        ReflectionTestUtils.setField(imageStorageService, "storageProvider", "local");
        ReflectionTestUtils.setField(imageStorageService, "localStorageDir", tempUploadDir.toString());
        ReflectionTestUtils.setField(imageStorageService, "maxFilesPerPost", 3);
        ReflectionTestUtils.setField(imageStorageService, "maxFileSizeBytes", 5L * 1024 * 1024);
        ReflectionTestUtils.setField(imageStorageService, "compressionQuality", 0.75);
        ReflectionTestUtils.setField(imageStorageService, "maxWidth", 1280);
        ReflectionTestUtils.setField(imageStorageService, "maxHeight", 1280);
    }

    @Test
    @DisplayName("Middleware chặn request khi tải lên nhiều hơn 3 ảnh / bài đăng")
    void shouldRejectWhenMoreThanThreeImagesInMiddleware() throws Exception {
        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        byte[] dummyImage = createSampleImageBytes(200, 200);
        for (int i = 1; i <= 4; i++) {
            request.addFile(new MockMultipartFile("files", "img" + i + ".jpg", "image/jpeg", dummyImage));
        }
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains("1012"));
    }

    @Test
    @DisplayName("Middleware chặn request khi dung lượng ảnh vượt quá 5MB")
    void shouldRejectWhenImageSizeExceedsFiveMegabytesInMiddleware() throws Exception {
        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        byte[] overFiveMb = new byte[5 * 1024 * 1024 + 100];
        request.addFile(new MockMultipartFile("files", "large.jpg", "image/jpeg", overFiveMb));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(413, response.getStatus());
        assertTrue(response.getContentAsString().contains("1013"));
    }

    @Test
    @DisplayName("Service nén ảnh tự động và lưu thành công khi hợp lệ (<= 3 ảnh, <= 5MB/ảnh)")
    void shouldCompressAndSaveValidImages() throws Exception {
        byte[] largeHighResPng = createSampleImageBytes(2000, 1500);
        MultipartFile file = new MockMultipartFile("files", "campus.png", "image/png", largeHighResPng);

        List<ImageUploadResponse> results = imageStorageService.uploadPostImagesWithDetails(List.of(file));

        assertEquals(1, results.size());
        ImageUploadResponse uploaded = results.get(0);
        assertEquals("LOCAL", uploaded.getStorageProvider());
        assertTrue(uploaded.getImageUrl().startsWith("/uploads/"));
        assertTrue(uploaded.getCompressedSizeBytes() < uploaded.getOriginalSizeBytes());
    }

    @Test
    @DisplayName("Service ném lỗi TOO_MANY_IMAGES khi truyền quá 3 ảnh")
    void shouldThrowExceptionWhenMoreThanThreeImagesInService() throws Exception {
        byte[] sampleBytes = createSampleImageBytes(100, 100);
        List<MultipartFile> fourFiles = List.of(
                new MockMultipartFile("files", "1.jpg", "image/jpeg", sampleBytes),
                new MockMultipartFile("files", "2.jpg", "image/jpeg", sampleBytes),
                new MockMultipartFile("files", "3.jpg", "image/jpeg", sampleBytes),
                new MockMultipartFile("files", "4.jpg", "image/jpeg", sampleBytes)
        );

        AppException ex = assertThrows(
                AppException.class,
                () -> imageStorageService.uploadPostImages(fourFiles)
        );
        assertEquals(ErrorCode.TOO_MANY_IMAGES, ex.getErrorCode());
    }

    private byte[] createSampleImageBytes(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        Random random = new Random(42);
        for (int y = 0; y < height; y += 10) {
            for (int x = 0; x < width; x += 10) {
                g2d.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
                g2d.fillRect(x, y, 10, 10);
            }
        }
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}

