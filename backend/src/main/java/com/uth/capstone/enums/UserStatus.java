package com.uth.capstone.enums;

import lombok.Getter;

/**
 * Trạng thái hoạt động của tài khoản người dùng (User).
 */
@Getter
public enum UserStatus {
    ACTIVE("Đang hoạt động"),
    LOCKED("Bị khóa truy cập");

    private final String description;

    UserStatus(String description) {
        this.description = description;
    }
}
