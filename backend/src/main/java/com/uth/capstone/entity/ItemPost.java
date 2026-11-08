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
 */
@Entity
@Table(name = "ItemPost")
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

