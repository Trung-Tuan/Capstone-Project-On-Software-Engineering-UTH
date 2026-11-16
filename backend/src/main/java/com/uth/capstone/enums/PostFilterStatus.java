package com.uth.capstone.enums;

import com.uth.capstone.exception.AppException;
import com.uth.capstone.exception.ErrorCode;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * Tiêu chí lọc trạng thái bài đăng trên API GET /posts (FR-SRCH-02):
 * - LOST (Cần tìm): ItemType = LOST và Status = APPROVED
 * - FOUND (Nhặt được): ItemType = FOUND và Status = APPROVED
 * - RETURNED (Đã hoàn trả): Status IN (RESOLVED, CLAIMED)
 * Ngoài ra hỗ trợ lọc trực tiếp theo PostStatus cụ thể (APPROVED, RESOLVED, CLAIMED, PENDING, EXPIRED, REJECTED) hoặc ALL.
 */
@Getter
public enum PostFilterStatus {
    LOST("Cần tìm"),
    FOUND("Nhặt được"),
    RETURNED("Đã hoàn trả"),
    APPROVED("Đã duyệt / Đang hiển thị"),
    RESOLVED("Đã giải quyết"),
    CLAIMED("Đã nhận lại"),
    PENDING("Chờ duyệt"),
    EXPIRED("Hết hạn"),
    REJECTED("Bị từ chối"),
    ALL("Tất cả trạng thái");

    private final String description;

    PostFilterStatus(String description) {
        this.description = description;
    }

    public static final List<PostStatus> DEFAULT_PUBLIC_STATUSES = List.of(
            PostStatus.APPROVED,
            PostStatus.CLAIMED,
            PostStatus.RESOLVED
    );

    public static final List<PostStatus> RETURNED_STATUSES = List.of(
            PostStatus.RESOLVED,
            PostStatus.CLAIMED
    );

    public static PostFilterStatus fromString(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "LOST", "NEED_TO_FIND", "CAN_TIM" -> LOST;
            case "FOUND", "NHAT_DUOC" -> FOUND;
            case "RETURNED", "COMPLETED", "DA_HOAN_TRA" -> RETURNED;
            case "APPROVED", "ACTIVE", "OPEN" -> APPROVED;
            case "RESOLVED" -> RESOLVED;
            case "CLAIMED" -> CLAIMED;
            case "PENDING" -> PENDING;
            case "EXPIRED" -> EXPIRED;
            case "REJECTED" -> REJECTED;
            case "ALL" -> ALL;
            default -> throw new AppException(ErrorCode.INVALID_FILTER_PARAMETER);
        };
    }
}
