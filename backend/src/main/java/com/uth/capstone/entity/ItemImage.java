package com.uth.capstone.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity ánh xạ bảng ItemImage (Hình ảnh đính kèm bài đăng) trong cơ sở dữ liệu MySQL (uth_find_db).
 */
@Entity
@Table(name = "ItemImage")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ImageID", nullable = false)
    private Integer imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PostID", nullable = false)
    private ItemPost post;

    @Column(name = "ImageUrl", nullable = false, length = 500)
    private String imageUrl;
}

