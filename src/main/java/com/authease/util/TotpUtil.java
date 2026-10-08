package com.authease.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public final class TotpUtil {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TIME_STEP_SECONDS = 30;

    private TotpUtil() {}

    public static String generateSecret(int numChars) {
        StringBuilder sb = new StringBuilder(numChars);
        for (int i = 0; i < numChars; i++) {
            sb.append(BASE32_ALPHABET.charAt(SECURE_RANDOM.nextInt(BASE32_ALPHABET.length())));
        }
        return sb.toString();
    }

    public static String generateCode(String base32Secret, long timeStep) {
        try {
            byte[] key = decodeBase32(base32Secret);
            byte[] data = ByteBuffer.allocate(8).putLong(timeStep).array();

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0xF;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % 1000000;
            return String.format("%06d", otp);
        } catch (Exception e) {
            throw new RuntimeException("Error computing TOTP", e);
        }
    }

    /**
     * Validates code within +-1 time step (30s window).
     * Returns matching timeStep if valid, or null if invalid.
     */
    public static Long validateCode(String base32Secret, String code, Long lastUsedTimeStep) {
        if (base32Secret == null || code == null || code.length() != 6) {
            return null;
        }

        long currentStep = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS;

        for (int window = -1; window <= 1; window++) {
            long testStep = currentStep + window;
            // Prevent reuse of code for the same time window
            if (lastUsedTimeStep != null && lastUsedTimeStep == testStep) {
                continue;
            }
            String expected = generateCode(base32Secret, testStep);
            if (CryptoUtil.constantTimeEquals(expected, code.trim())) {
                return testStep;
            }
        }
        return null;
    }

    public static String buildOtpAuthUrl(String email, String secret) {
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        return String.format("otpauth://totp/AuthEase:%s?secret=%s&issuer=AuthEase&algorithm=SHA1&digits=6&period=30",
                encodedEmail, secret);
    }

    public static String generateQrDataUrl(String otpAuthUrl) {
        // Embed Google Charts / quickchart or SVG QR fallback
        // Return an SVG Data URL representing a QR placeholder / visual code for instant client rendering
        String encodedUrl = URLEncoder.encode(otpAuthUrl, StandardCharsets.UTF_8);
        return "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + encodedUrl;
    }

    public static byte[] decodeBase32(String base32) {
        String clean = base32.toUpperCase().replaceAll("[^A-Z2-7]", "");
        int numBytes = clean.length() * 5 / 8;
        byte[] bytes = new byte[numBytes];
        int buffer = 0;
        int bitsLeft = 0;
        int byteIndex = 0;

        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            int val = BASE32_ALPHABET.indexOf(c);
            if (val < 0) continue;

            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                if (byteIndex < bytes.length) {
                    bytes[byteIndex++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                }
                bitsLeft -= 8;
            }
        }
        return bytes;
    }
}
