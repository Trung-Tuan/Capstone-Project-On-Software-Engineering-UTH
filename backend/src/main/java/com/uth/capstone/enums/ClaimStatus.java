package com.uth.capstone.enums;

import lombok.Getter;

/**
 * Trạng thái của yêu cầu xác minh nhận lại đồ (ClaimRequest).
 */
@Getter
public enum ClaimStatus {
    SUBMITTED("Đã gửi yêu cầu / Chờ xác minh"),
    ACCEPTED("Đã chấp nhận yêu cầu"),
    REJECTED("Bị từ chối yêu cầu");

    private final String description;

    ClaimStatus(String description) {
        this.description = description;
    }
}

