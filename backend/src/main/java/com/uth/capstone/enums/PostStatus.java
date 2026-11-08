package com.uth.capstone.enums;

import lombok.Getter;

/**
 * Trạng thái của bài đăng tin tìm/nhặt đồ (ItemPost).
 */
@Getter
public enum PostStatus {
    PENDING("Chờ duyệt"),
    APPROVED("Đã duyệt / Đang hiển thị"),
    CLAIMED("Đã nhận lại đồ"),
    RESOLVED("Đã giải quyết / Đã tìm thấy chủ"),
    EXPIRED("Hết hạn hiển thị"),
    REJECTED("Bị từ chối duyệt");

    private final String description;

    PostStatus(String description) {
        this.description = description;
    }
}
