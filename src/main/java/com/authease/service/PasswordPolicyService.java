package com.authease.service;

import com.authease.dto.PasswordCheckResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
public class PasswordPolicyService {

    private static final Logger log = LoggerFactory.getLogger(PasswordPolicyService.class);
    private static final String HIBP_RANGE_URL = "https://api.pwnedpasswords.com/range/";

    public PasswordCheckResponse evaluatePassword(String password) {
        if (password == null) {
            return new PasswordCheckResponse(0, List.of("Password cannot be empty."), false);
        }

        List<String> hints = new ArrayList<>();
        int len = password.length();

        // NIST SP 800-63B Rules: Min 10, Max 128 characters
        if (len < 10) {
            hints.add("Password must be at least 10 characters long.");
        } else if (len > 128) {
            hints.add("Password cannot exceed 128 characters.");
        }

        // Check Have I Been Pwned range API using k-anonymity
        boolean breached = isBreached(password);
        if (breached) {
            hints.add("This password was found in public data breaches. Choose a different password or passphrase.");
        }

        // Calculate 0-4 entropy / strength score
        int score = calculateScore(password, breached);

        if (hints.isEmpty()) {
            hints.add("Good password! It is easy for you to remember and hard for attackers to guess.");
        }

        return new PasswordCheckResponse(score, hints, breached);
    }

    public void validatePasswordOrThrow(String password) {
        if (password == null || password.length() < 10 || password.length() > 128) {
            throw new UserService.CustomAuthException(
                    "VALIDATION_ERROR",
                    "Password length requirement",
                    "Password must be between 10 and 128 characters.",
                    "Choose a memorable passphrase with at least 10 characters."
            );
        }

        if (isBreached(password)) {
            throw new UserService.CustomAuthException(
                    "VALIDATION_ERROR",
                    "Weak or compromised password",
                    "This password has appeared in previous data breaches.",
                    "Choose a unique phrase that you have not used on other websites."
            );
        }
    }

    private int calculateScore(String password, boolean breached) {
        int len = password.length();
        if (len < 10) return 0;
        if (breached) return 1;

        int score = 2;
        if (len >= 14) score++;
        if (len >= 18) score++;

        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));

        int variety = (hasUpper ? 1 : 0) + (hasLower ? 1 : 0) + (hasDigit ? 1 : 0) + (hasSpecial ? 1 : 0);
        if (variety >= 3 && score < 4) {
            score++;
        }

        return Math.min(4, Math.max(0, score));
    }

    public boolean isBreached(String password) {
        if (password == null || password.isEmpty()) return false;
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hash = sha1.digest(password.getBytes(StandardCharsets.UTF_8));
            String fullHash = HexFormat.of().formatHex(hash).toUpperCase();

            String prefix = fullHash.substring(0, 5);
            String suffix = fullHash.substring(5);

            URL url = new URL(HIBP_RANGE_URL + prefix);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "AuthEase-PasswordChecker");
            conn.setConnectTimeout(2000); // 2s timeout
            conn.setReadTimeout(2000);

            int status = conn.getResponseCode();
            if (status != 200) {
                log.warn("Have I Been Pwned API returned HTTP {}. Failing open.", status);
                return false;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int colonIdx = line.indexOf(':');
                    if (colonIdx > 0) {
                        String entrySuffix = line.substring(0, colonIdx).trim().toUpperCase();
                        if (entrySuffix.equals(suffix)) {
                            return true; // Match found: compromised
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Have I Been Pwned API unreachable ({}), failing open as per policy.", e.getMessage());
        }
        return false;
    }
}
