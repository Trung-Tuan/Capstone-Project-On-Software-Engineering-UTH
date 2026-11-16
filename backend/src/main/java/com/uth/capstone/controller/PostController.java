package com.uth.capstone.controller;

import com.uth.capstone.dto.request.UpdatePostRequest;
import com.uth.capstone.dto.request.UpdatePostStatusRequest;
import com.uth.capstone.dto.response.ApiResponse;
import com.uth.capstone.dto.response.PageResponse;
import com.uth.capstone.dto.response.PostResponse;
import com.uth.capstone.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Controller cung cấp các API quản lý & tìm kiếm bài đăng Mất đồ / Nhặt được:
 * - GET /posts (& /api/posts): Tìm kiếm Full-text trên Title & Description, bộ lọc kết hợp 4 tiêu chí & sắp xếp.
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
        description = "API Tìm kiếm, Lọc kết hợp, Sắp xếp và Quản lý bài đăng Mất đồ / Nhặt được"
)
public class PostController {

    private final PostService postService;

    @GetMapping
    @Operation(
            summary = "Tìm kiếm Full-text, lọc kết hợp 4 tiêu chí và sắp xếp danh sách bài đăng",
            description = """
                    Hỗ trợ tìm kiếm theo từ khóa bằng Full-text Index trên Title & Description, kết hợp 4 tiêu chí lọc:
                    1. Trạng thái (status): LOST (Cần tìm), FOUND (Nhặt được), RETURNED (Đã hoàn trả - RESOLVED/CLAIMED) hoặc các giá trị PostStatus cụ thể.
                    2. Danh mục (categoryId).
                    3. Khu vực / Tòa nhà (locationId hoặc buildingName).
                    4. Khoảng thời gian (fromDate / dateFrom - toDate / dateTo, định dạng yyyy-MM-dd hoặc yyyy-MM-ddTHH:mm:ss).
                    Hỗ trợ sắp xếp (sortBy): NEWEST (Mới nhất), OLDEST (Cũ nhất), RELEVANCE (Độ liên quan của từ khóa).
                    """
    )
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> searchPosts(
            @Parameter(description = "Từ khóa tìm kiếm Full-text trên Title và Description")
            @RequestParam(name = "keyword", required = false) String keyword,

            @Parameter(description = "Alias cho keyword")
            @RequestParam(name = "q", required = false) String q,

            @Parameter(description = "Tiêu chí 1 - Trạng thái: LOST (Cần tìm), FOUND (Nhặt được), RETURNED (Đã hoàn trả), hoặc APPROVED/RESOLVED/CLAIMED/PENDING/EXPIRED/REJECTED/ALL")
            @RequestParam(name = "status", required = false) String status,

            @Parameter(description = "Lọc bổ sung theo loại bài đăng: LOST hoặc FOUND")
            @RequestParam(name = "itemType", required = false) String itemType,

            @Parameter(description = "Alias cho itemType")
            @RequestParam(name = "type", required = false) String type,

            @Parameter(description = "Tiêu chí 2 - ID danh mục đồ vật (CategoryID)")
            @RequestParam(name = "categoryId", required = false) Integer categoryId,

            @Parameter(description = "Tiêu chí 3 - ID khu vực / địa điểm (LocationID)")
            @RequestParam(name = "locationId", required = false) Integer locationId,

            @Parameter(description = "Tiêu chí 3 (tùy chọn) - Lọc theo tên tòa nhà (BuildingName)")
            @RequestParam(name = "buildingName", required = false) String buildingName,

            @Parameter(description = "Tiêu chí 4 - Từ ngày (yyyy-MM-dd hoặc yyyy-MM-ddTHH:mm:ss)")
            @RequestParam(name = "fromDate", required = false) String fromDate,

            @Parameter(description = "Alias cho fromDate")
            @RequestParam(name = "dateFrom", required = false) String dateFrom,

            @Parameter(description = "Tiêu chí 4 - Đến ngày (yyyy-MM-dd hoặc yyyy-MM-ddTHH:mm:ss)")
            @RequestParam(name = "toDate", required = false) String toDate,

            @Parameter(description = "Alias cho toDate")
            @RequestParam(name = "dateTo", required = false) String dateTo,

            @Parameter(description = "Sắp xếp kết quả: NEWEST (Mới nhất), OLDEST (Cũ nhất), RELEVANCE (Độ liên quan từ khóa)")
            @RequestParam(name = "sortBy", required = false) String sortBy,

            @Parameter(description = "Số trang (bắt đầu từ 0)")
            @RequestParam(name = "page", defaultValue = "0") int page,

            @Parameter(description = "Số bản ghi mỗi trang (mặc định 12, tối đa 100)")
            @RequestParam(name = "size", defaultValue = "12") int size,

            @Parameter(description = "Alias cho size")
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        String effectiveKeyword = StringUtils.hasText(keyword) ? keyword : q;
        String effectiveItemType = StringUtils.hasText(itemType) ? itemType : type;
        String effectiveFromDate = StringUtils.hasText(fromDate) ? fromDate : dateFrom;
        String effectiveToDate = StringUtils.hasText(toDate) ? toDate : dateTo;
        int effectiveSize = limit != null ? limit : size;

        PageResponse<PostResponse> result = postService.searchPosts(
                effectiveKeyword,
                status,
                effectiveItemType,
                categoryId,
                locationId,
                buildingName,
                effectiveFromDate,
                effectiveToDate,
                sortBy,
                page,
                effectiveSize
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

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
