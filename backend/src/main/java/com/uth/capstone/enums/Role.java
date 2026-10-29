package com.uth.capstone.enums;

import lombok.Getter;

/**
 * Vai trò phân quyền của người dùng trong hệ thống (RBAC).
 */
@Getter
public enum Role {
    STUDENT("Sinh viên"),
    STAFF("Giảng viên / Cán bộ nhân viên"),
    ADMIN("Quản trị viên hệ thống"),
    SECURITY("Đội Bảo vệ"),
    SAC("Ban Công tác sinh viên");

    private final String description;

    Role(String description) {
        this.description = description;
    }
}
