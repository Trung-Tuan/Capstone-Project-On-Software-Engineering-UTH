package com.uth.capstone.controller;

import com.uth.capstone.dto.request.UpdatePostRequest;
import com.uth.capstone.dto.request.UpdatePostStatusRequest;
import com.uth.capstone.dto.response.ApiResponse;
import com.uth.capstone.dto.response.PostResponse;
import com.uth.capstone.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller cung cấp các API quản lý bài đăng Mất đồ / Nhặt được:
 * - GET /posts/{id} (& /api/posts/{id}): Xem chi tiết bài đăng và ẩn PhoneNumber của người đăng trên dữ liệu công khai.
 * - PUT /posts/{id} (& /api/posts/{id}): Chỉ cho phép tác giả chỉnh sửa bài đăng của chính mình khi bài chưa đóng.
 * - DELETE /posts/{id} (& /api/posts/{id}): Cho phép tác giả xóa bài đăng của chính mình.
 * - PATCH /posts/{id}/status (& /api/posts/{id}/status): Cho phép tác giả đổi trạng thái thành "Đã nhận lại" (RESOLVED / CLAIMED).
 */
@RestController
@RequestMapping({"/api/posts", "/posts"})
@RequiredArgsConstructor
@Tag(
        name = "Posts",
        description = "API Quản lý bài đăng Mất đồ / Nhặt được (Xem chi tiết, Chỉnh sửa, Xóa, Đổi trạng thái Đã nhận lại)"
)
public class PostController {

    private final PostService postService;

    @GetMapping("/{id}")
    @Operation(
            summary = "Lấy chi tiết bài đăng theo ID (Ẩn PhoneNumber công khai)",
            description = "Trả về thông tin chi tiết bài đăng kèm danh mục, địa điểm, hình ảnh và thông tin công khai của người đăng (ẩn hoàn toàn trường số điện thoại cá nhân PhoneNumber)."
    )
    public ResponseEntity<ApiResponse<PostResponse>> getPostById(
            @PathVariable("id") Integer id
    ) {
        PostResponse postResponse = postService.getPostById(id);
        return ResponseEntity.ok(ApiResponse.success(postResponse));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Chỉnh sửa bài đăng (Chỉ tác giả và khi bài chưa đóng)",
            description = "Chỉ cho phép tác giả chỉnh sửa bài đăng của chính mình khi bài đăng đang mở (PENDING hoặc APPROVED, chưa bị đóng/RESOLVED/CLAIMED/EXPIRED/REJECTED)."
    )
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable("id") Integer id,
            @Valid @RequestBody UpdatePostRequest request
    ) {
        PostResponse updatedPost = postService.updatePost(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật bài đăng thành công", updatedPost));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Xóa bài đăng (Chỉ tác giả của bài đăng)",
            description = "Cho phép tác giả xóa bài đăng của chính mình."
    )
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable("id") Integer id
    ) {
        postService.deletePost(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa bài đăng thành công", null));
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Đổi trạng thái bài đăng thành 'Đã nhận lại' (RESOLVED / CLAIMED)",
            description = "Cho phép tác giả đánh dấu bài đăng của chính mình sang trạng thái Đã nhận lại (RESOLVED hoặc CLAIMED)."
    )
    public ResponseEntity<ApiResponse<PostResponse>> updatePostStatus(
            @PathVariable("id") Integer id,
            @RequestBody(required = false) UpdatePostStatusRequest request
    ) {
        PostResponse updatedPost = postService.updatePostStatusByAuthor(id, request);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật trạng thái bài đăng thành Đã nhận lại", updatedPost));
    }
}

