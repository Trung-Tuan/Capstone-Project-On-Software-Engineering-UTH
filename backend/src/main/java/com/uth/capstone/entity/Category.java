package com.uth.capstone.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity ánh xạ bảng Category (Danh mục đồ vật) trong cơ sở dữ liệu MySQL (uth_find_db).
 */
@Entity
@Table(name = "Category")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CategoryID", nullable = false)
    private Integer categoryId;

    @Column(name = "CategoryName", nullable = false, unique = true, length = 100)
    private String categoryName;

    @Column(name = "Icon", length = 255)
    private String icon;
}

