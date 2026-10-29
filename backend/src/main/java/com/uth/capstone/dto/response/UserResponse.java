package com.uth.capstone.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.uth.capstone.enums.Role;
import com.uth.capstone.enums.UserStatus;
import lombok.*;

/**
 * DTO thông tin tài khoản người dùng trả về cho Client.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private Integer userId;
    private String email;
    private String fullName;
    private String studentId;
    private String phoneNumber;
    private Role role;
    private UserStatus status;
}
