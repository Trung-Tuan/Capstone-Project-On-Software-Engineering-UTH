package com.uth.capstone.dto.response;

import lombok.*;

/**
 * DTO trả về sau khi xác thực Google OAuth2 thành công, bao gồm JWT Access Token và thông tin User.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private long expiresIn;

    private UserResponse user;
}
