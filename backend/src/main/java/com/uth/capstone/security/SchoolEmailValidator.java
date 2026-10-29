package com.uth.capstone.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Kiểm tra ràng buộc đuôi email trường học (ví dụ: @ut.edu.vn, @uth.edu.vn).
 */
@Component
public class SchoolEmailValidator {

    private final List<String> allowedDomains;

    public SchoolEmailValidator(
            @Value("${app.security.allowed-email-domains:ut.edu.vn,uth.edu.vn}") String allowedDomainsConfig
    ) {
        this.allowedDomains = Arrays.stream(allowedDomainsConfig.split(","))
                .map(String::trim)
                .map(d -> d.startsWith("@") ? d.substring(1).toLowerCase(Locale.ROOT) : d.toLowerCase(Locale.ROOT))
                .filter(d -> !d.isEmpty())
                .toList();
    }

    public boolean isValidSchoolEmail(String email) {
        if (email == null || !email.contains("@")) {
            return false;
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String domain = normalizedEmail.substring(normalizedEmail.lastIndexOf('@') + 1);

        for (String allowedDomain : allowedDomains) {
            if (domain.equals(allowedDomain) || domain.endsWith("." + allowedDomain)) {
                return true;
            }
        }
        return false;
    }
}
