package com.uth.capstone.dto.request;

import com.uth.capstone.enums.PostStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO yêu cầu cập nhật trạng thái bài đăng sang "Đã nhận lại" (RESOLVED / CLAIMED) (PATCH /posts/:id/status).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostStatusRequest {

    @NotNull(message = "Trạng thái bài đăng mới không được để trống (RESOLVED hoặc CLAIMED)")
    private PostStatus status;
}

