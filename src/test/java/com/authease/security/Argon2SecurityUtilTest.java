package com.authease.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Argon2SecurityUtilTest {

    private Argon2SecurityUtil securityUtil;

    @BeforeEach
    void setUp() {
        securityUtil = new Argon2SecurityUtil();
    }

    @Test
    void testEncodeAndMatches() {
        String rawPassword = "SuperSecurePassword123!";
        String encoded = securityUtil.encode(rawPassword);

        assertNotNull(encoded);
        assertTrue(securityUtil.matches(rawPassword, encoded));
        assertFalse(securityUtil.matches("WrongPassword123!", encoded));
    }

    @Test
    void testVerifyAgainstDummyHash_DoesNotThrow() {
        assertDoesNotThrow(() -> securityUtil.verifyAgainstDummyHash("anyPasswordAttempt"));
    }
}
