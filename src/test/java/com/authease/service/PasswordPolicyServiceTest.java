package com.authease.service;

import com.authease.dto.PasswordCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyServiceTest {

    private PasswordPolicyService passwordPolicyService;

    @BeforeEach
    void setUp() {
        passwordPolicyService = new PasswordPolicyService();
    }

    @Test
    void testShortPassword_FailsValidation() {
        assertThrows(UserService.CustomAuthException.class, () ->
                passwordPolicyService.validatePasswordOrThrow("short123"));

        PasswordCheckResponse check = passwordPolicyService.evaluatePassword("short");
        assertEquals(0, check.getScore());
        assertTrue(check.getHints().stream().anyMatch(h -> h.contains("at least 10 characters")));
    }

    @Test
    void testNistCompliantPassword_AllowsUnicodeAndSpaces() {
        // NIST SP 800-63B passphrase with spaces and unicode symbols
        String passphrase = "correct horse battery staple 🚀";
        assertDoesNotThrow(() -> passwordPolicyService.validatePasswordOrThrow(passphrase));

        PasswordCheckResponse check = passwordPolicyService.evaluatePassword(passphrase);
        assertTrue(check.getScore() >= 3);
        assertFalse(check.isBreached());
    }

    @Test
    void testPasswordExceedingMax128Chars_FailsValidation() {
        String longPass = "a".repeat(129);
        assertThrows(UserService.CustomAuthException.class, () ->
                passwordPolicyService.validatePasswordOrThrow(longPass));
    }
}
