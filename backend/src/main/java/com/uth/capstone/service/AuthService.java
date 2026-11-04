package com.uth.capstone.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.uth.capstone.dto.request.GoogleLoginRequest;
import com.uth.capstone.dto.response.AuthResponse;
import com.uth.capstone.dto.response.UserResponse;
import com.uth.capstone.entity.User;
import com.uth.capstone.enums.Role;
import com.uth.capstone.enums.UserStatus;
import com.uth.capstone.exception.AppException;
import com.uth.capstone.exception.ErrorCode;
import com.uth.capstone.repository.UserRepository;
import com.uth.capstone.security.CustomUserDetails;
import com.uth.capstone.security.JwtTokenProvider;
import com.uth.capstone.security.SchoolEmailValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service xử lý xác thực Google Identity Services (OAuth2), kiểm tra email trường và cấp phát JWT.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("(\\d{8,12})");

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final SchoolEmailValidator schoolEmailValidator;

    @Value("${app.google.client-id:}")
    private String googleClientId;

    @Transactional
    public AuthResponse authenticateWithGoogle(GoogleLoginRequest request) {
        GoogleIdToken.Payload payload = verifyGoogleIdToken(request.getIdToken());

        String email = payload.getEmail() != null
                ? payload.getEmail().trim().toLowerCase(Locale.ROOT)
                : null;

        if (!StringUtils.hasText(email) || Boolean.FALSE.equals(payload.getEmailVerified())) {
            throw new AppException(ErrorCode.INVALID_GOOGLE_TOKEN);
        }

        // Kiểm tra ràng buộc đuôi email trường (@ut.edu.vn)
        if (!schoolEmailValidator.isValidSchoolEmail(email)) {
            throw new AppException(ErrorCode.INVALID_SCHOOL_EMAIL);
        }

        String googleName = (String) payload.get("name");
        String fullName = StringUtils.hasText(googleName)
                ? googleName.trim()
                : email.substring(0, email.indexOf('@'));

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createNewUser(email, fullName));

        // Chặn đăng nhập nếu tài khoản đã bị khóa (FR-ADM-04)
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        if (StringUtils.hasText(fullName) && !fullName.equals(user.getFullName())) {
            user.setFullName(fullName);
            user = userRepository.save(user);
        }

        String accessToken = jwtTokenProvider.generateToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getJwtExpirationMs() / 1000)
                .user(toUserResponse(user))
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        return toUserResponse(user);
    }

    private GoogleIdToken.Payload verifyGoogleIdToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier.Builder builder = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            );

            if (StringUtils.hasText(googleClientId)) {
                builder.setAudience(Collections.singletonList(googleClientId.trim()));
            }

            GoogleIdToken googleIdToken = builder.build().verify(idTokenString);
            if (googleIdToken == null || googleIdToken.getPayload() == null) {
                throw new AppException(ErrorCode.INVALID_GOOGLE_TOKEN);
            }
            return googleIdToken.getPayload();
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AppException(ErrorCode.INVALID_GOOGLE_TOKEN);
        }
    }

    private User createNewUser(String email, String fullName) {
        String extractedStudentId = extractStudentIdFromEmail(email);
        if (extractedStudentId != null && userRepository.findByStudentId(extractedStudentId).isPresent()) {
            extractedStudentId = null;
        }

        User newUser = User.builder()
                .email(email)
                .fullName(fullName)
                .studentId(extractedStudentId)
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .build();

        return userRepository.save(newUser);
    }

    private String extractStudentIdFromEmail(String email) {
        String localPart = email.substring(0, email.indexOf('@'));
        Matcher matcher = STUDENT_ID_PATTERN.matcher(localPart);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .studentId(user.getStudentId())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }
}
