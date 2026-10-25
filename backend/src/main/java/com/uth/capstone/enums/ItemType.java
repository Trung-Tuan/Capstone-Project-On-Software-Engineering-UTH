package com.uth.capstone.enums;

import lombok.Getter;

/**
 * Loại bài đăng đồ dùng: Thất lạc hoặc Nhặt được.
 */
@Getter
public enum ItemType {
    LOST("Đồ bị mất / Thất lạc"),
    FOUND("Đồ nhặt được");

    private final String description;

    ItemType(String description) {
        this.description = description;
    }
}

