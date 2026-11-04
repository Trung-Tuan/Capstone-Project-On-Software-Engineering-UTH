package com.uth.capstone.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * DTO nhận Google ID Token từ Google Identity Services ở phía Frontend.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginRequest {

    @NotBlank(message = "Google ID Token không được để trống")
    @JsonAlias({"credential", "token"})
    private String idToken;
}
