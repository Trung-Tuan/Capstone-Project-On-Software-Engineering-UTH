package com.uth.capstone.entity;

import com.uth.capstone.enums.ItemType;
import com.uth.capstone.enums.PostStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity ánh xạ bảng ItemPost (Bài đăng Mất đồ / Nhặt được) trong cơ sở dữ liệu MySQL (uth_find_db).
 * Bảng đã được đánh Full-text Index `ft_itempost_search` trên (Title, Description)
 * cùng các B-tree composite index phục vụ bộ lọc kết hợp & sắp xếp nhanh với 50.000+ bản ghi.
 */
@Entity
@Table(
        name = "ItemPost",
        indexes = {
                @Index(name = "idx_itempost_filter", columnList = "Status, ItemType, CategoryID, LocationID, IncidentDate"),
                @Index(name = "idx_itempost_created_at", columnList = "CreatedAt"),
                @Index(name = "idx_itempost_status_created", columnList = "Status, CreatedAt, PostID"),
                @Index(name = "idx_itempost_type_status_created", columnList = "ItemType, Status, CreatedAt, PostID"),
                @Index(name = "idx_itempost_category_status_created", columnList = "CategoryID, Status, CreatedAt, PostID"),
                @Index(name = "idx_itempost_location_status_created", columnList = "LocationID, Status, CreatedAt, PostID"),
                @Index(name = "idx_itempost_incident_status", columnList = "IncidentDate, Status, PostID")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PostID", nullable = false)
    private Integer postId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = false)
    private User user;

    @Column(name = "Title", nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "ItemType", nullable = false, columnDefinition = "enum('LOST','FOUND')")
    private ItemType itemType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CategoryID", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LocationID", nullable = false)
    private Location location;

    @Column(name = "IncidentDate", nullable = false)
    private LocalDateTime incidentDate;

    @Column(name = "Description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "Status",
            nullable = false,
            columnDefinition = "enum('PENDING','APPROVED','CLAIMED','RESOLVED','EXPIRED','REJECTED')"
    )
    @Builder.Default
    private PostStatus status = PostStatus.PENDING;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemImage> images = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = PostStatus.PENDING;
        }
    }
}

