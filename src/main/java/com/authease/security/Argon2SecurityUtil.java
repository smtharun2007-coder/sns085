package com.authease.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Argon2SecurityUtil {

    private static final Logger log = LoggerFactory.getLogger(Argon2SecurityUtil.class);

    private final PasswordEncoder passwordEncoder;
    private final String dummyHash;

    public Argon2SecurityUtil() {
        PasswordEncoder encoder;
        try {
            // Argon2 parameters: saltLength 16, hashLength 32, parallelism 1, memory 16384, iterations 2
            encoder = new Argon2PasswordEncoder(16, 32, 1, 16384, 2);
            // Test hashing once to ensure BouncyCastle provider is active
            String testHash = encoder.encode("testPassword");
            encoder.matches("testPassword", testHash);
            log.info("Initialized Argon2PasswordEncoder with BouncyCastle provider.");
        } catch (Throwable t) {
            log.warn("Argon2 initialization failed or BouncyCastle unavailable ({}), falling back to BCrypt.", t.getMessage());
            encoder = new BCryptPasswordEncoder(12);
        }
        this.passwordEncoder = encoder;
        this.dummyHash = encoder.encode("dummy-password-for-timing-uniformity-only-12345");
    }

    public PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    public String encode(CharSequence rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public void verifyAgainstDummyHash(CharSequence rawPassword) {
        // Prevents timing attacks on nonexistent accounts
        try {
            passwordEncoder.matches(rawPassword, dummyHash);
        } catch (Exception ignored) {}
    }
}
