package com.authease.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilTest {

    @Test
    void testSha256() {
        String hash1 = CryptoUtil.sha256("test@example.com");
        String hash2 = CryptoUtil.sha256("test@example.com");
        String hash3 = CryptoUtil.sha256("other@example.com");

        assertNotNull(hash1);
        assertEquals(64, hash1.length());
        assertEquals(hash1, hash2);
        assertNotEquals(hash1, hash3);
    }

    @Test
    void testConstantTimeEquals() {
        assertTrue(CryptoUtil.constantTimeEquals("secret123", "secret123"));
        assertFalse(CryptoUtil.constantTimeEquals("secret123", "secret124"));
        assertFalse(CryptoUtil.constantTimeEquals(null, "secret123"));
        assertFalse(CryptoUtil.constantTimeEquals("secret123", null));
    }

    @Test
    void testAesGcmEncryptionDecryption() {
        String key = "0123456789abcdef0123456789abcdef";
        String secret = "JBSWY3DPEHPK3PXP";

        String encrypted = CryptoUtil.encryptAesGcm(secret, key);
        assertNotNull(encrypted);
        assertNotEquals(secret, encrypted);

        String decrypted = CryptoUtil.decryptAesGcm(encrypted, key);
        assertEquals(secret, decrypted);
    }

    @Test
    void testRandomTokenGeneration() {
        String token1 = CryptoUtil.generateRandomToken(32);
        String token2 = CryptoUtil.generateRandomToken(32);

        assertEquals(64, token1.length()); // 32 bytes in hex = 64 characters
        assertNotEquals(token1, token2);
    }
}
