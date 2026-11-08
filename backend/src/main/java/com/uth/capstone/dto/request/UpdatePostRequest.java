package com.uth.capstone.dto.request;

import com.uth.capstone.enums.ItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO yêu cầu chỉnh sửa bài đăng (PUT /posts/:id).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostRequest {

    @NotBlank(message = "Tiêu đề bài đăng không được để trống")
    @Size(max = 255, message = "Tiêu đề bài đăng không được vượt quá 255 ký tự")
    private String title;

    @NotNull(message = "Loại bài đăng (LOST hoặc FOUND) không được để trống")
    private ItemType itemType;

    @NotNull(message = "Danh mục đồ vật (categoryId) không được để trống")
    private Integer categoryId;

    @NotNull(message = "Địa điểm (locationId) không được để trống")
    private Integer locationId;

    @NotNull(message = "Thời điểm mất/nhặt đồ (incidentDate) không được để trống")
    private LocalDateTime incidentDate;

    @NotBlank(message = "Mô tả chi tiết không được để trống")
    private String description;

    @Size(max = 3, message = "Mỗi bài đăng chỉ được phép đính kèm tối đa 3 ảnh")
    private List<String> imageUrls;
}

