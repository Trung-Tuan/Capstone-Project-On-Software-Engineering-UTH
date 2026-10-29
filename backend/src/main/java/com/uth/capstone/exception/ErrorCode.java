package com.uth.capstone.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi không xác định từ hệ thống", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Mã lỗi không hợp lệ", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "Người dùng đã tồn tại", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1003, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1004, "Chưa xác thực đăng nhập", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1005, "Bạn không có quyền thực hiện hành động này", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND(1006, "Không tìm thấy tài nguyên yêu cầu", HttpStatus.NOT_FOUND),
    INVALID_REQUEST(1007, "Dữ liệu yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_GOOGLE_TOKEN(1008, "Google ID Token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),
    INVALID_SCHOOL_EMAIL(1009, "Chỉ chấp nhận tài khoản email trường học (@ut.edu.vn)", HttpStatus.FORBIDDEN),
    USER_LOCKED(1010, "Tài khoản của bạn đã bị khóa do vi phạm quy định sử dụng", HttpStatus.FORBIDDEN),
    INVALID_TOKEN(1011, "Token xác thực không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED);

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
