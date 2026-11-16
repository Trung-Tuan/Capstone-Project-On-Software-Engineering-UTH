package com.uth.capstone.dto.request;

import com.uth.capstone.enums.ItemType;
import com.uth.capstone.enums.PostSortBy;
import com.uth.capstone.enums.PostStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO chứa các tiêu chí tìm kiếm Full-text, bộ lọc kết hợp 4 tiêu chí và tùy chọn sắp xếp, phân trang cho API GET /posts.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostSearchRequest {

    /**
     * Từ khóa tìm kiếm Full-text trên Title và Description.
     */
    private String keyword;

    /**
     * Tiêu chí 1: Danh sách trạng thái bài đăng cần lọc (tương ứng Cần tìm / Nhặt được / Đã hoàn trả hoặc trạng thái cụ thể).
     */
    private List<PostStatus> statuses;

    /**
     * Tiêu chí 1 (bổ sung): Loại bài đăng (LOST = Cần tìm, FOUND = Nhặt được).
     */
    private ItemType itemType;

    /**
     * Tiêu chí 2: Danh mục đồ vật (CategoryID).
     */
    private Integer categoryId;

    /**
     * Tiêu chí 3: Khu vực / Địa điểm cụ thể (LocationID).
     */
    private Integer locationId;

    /**
     * Tiêu chí 3 (bổ sung): Tên tòa nhà (BuildingName) khi người dùng lọc theo cả tòa nhà.
     */
    private String buildingName;

    /**
     * Tiêu chí 4: Thời điểm bắt đầu (Từ ngày - IncidentDate >= fromDateTime).
     */
    private LocalDateTime fromDateTime;

    /**
     * Tiêu chí 4: Thời điểm kết thúc (Đến ngày - IncidentDate < toDateTime hoặc <= toDateTime).
     */
    private LocalDateTime toDateTime;

    /**
     * True nếu toDate được truyền dạng yyyy-MM-dd (so sánh < ngày kế tiếp để bao trọn 23:59:59 không bị làm tròn DATETIME).
     */
    private boolean toDateExclusive;

    /**
     * Tiêu chí sắp xếp: NEWEST (Mới nhất), OLDEST (Cũ nhất), RELEVANCE (Độ liên quan của từ khóa).
     */
    @Builder.Default
    private PostSortBy sortBy = PostSortBy.NEWEST;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 12;
}
