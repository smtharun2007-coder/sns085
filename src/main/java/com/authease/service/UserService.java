package com.authease.service;

import com.authease.dto.ErrorResponse;
import com.authease.dto.GenericResponse;
import com.authease.dto.RegisterRequest;
import com.authease.model.*;
import com.authease.repository.EmailTokenRepository;
import com.authease.repository.UserRepository;
import com.authease.security.Argon2SecurityUtil;
import com.authease.util.CryptoUtil;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final EmailTokenRepository emailTokenRepository;
    private final Argon2SecurityUtil passwordEncoder;
    private final EmailService emailService;
    private final AuditService auditService;
    private final ReasonCatalogService reasonCatalogService;

    public UserService(UserRepository userRepository,
                       EmailTokenRepository emailTokenRepository,
                       Argon2SecurityUtil passwordEncoder,
                       EmailService emailService,
                       AuditService auditService,
                       ReasonCatalogService reasonCatalogService) {
        this.userRepository = userRepository;
        this.emailTokenRepository = emailTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.auditService = auditService;
        this.reasonCatalogService = reasonCatalogService;
    }

    public GenericResponse register(RegisterRequest request, String demoSessionId, String clientIp) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        Optional<User> existingUser = userRepository.findByEmail(normalizedEmail);
        if (existingUser.isPresent()) {
            // Generic response to prevent account enumeration, but notify account holder
            emailService.sendAlreadyRegisteredEmail(demoSessionId, normalizedEmail);
            return GenericResponse.ok();
        }

        // Create new user
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(normalizedEmail, request.getDisplayName().trim(), encodedPassword);
        user.setEmailVerified(false);
        userRepository.save(user);

        // Generate 32-byte (256-bit) single-use verification token
        String rawToken = CryptoUtil.generateRandomToken(32);
        String tokenHash = CryptoUtil.sha256(rawToken);
        Instant expiresAt = Instant.now().plus(Duration.ofHours(24));

        EmailToken emailToken = new EmailToken(user.getId(), TokenType.VERIFY, tokenHash, expiresAt);
        emailTokenRepository.save(emailToken);

        // Send verification link
        emailService.sendVerificationEmail(demoSessionId, normalizedEmail, rawToken);

        return GenericResponse.ok();
    }

    public GenericResponse verifyEmail(String rawToken, String clientIp) {
        String tokenHash = CryptoUtil.sha256(rawToken.trim());
        Optional<EmailToken> optionalToken = emailTokenRepository.findByTokenHashAndType(tokenHash, TokenType.VERIFY);

        if (optionalToken.isEmpty()) {
            ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("TOKEN_INVALID_OR_EXPIRED");
            throw new CustomAuthException(detail.reasonCode(), detail.title(), detail.message(), detail.nextStep());
        }

        EmailToken emailToken = optionalToken.get();
        if (emailToken.getUsedAt() != null || emailToken.getExpiresAt().isBefore(Instant.now())) {
            ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("TOKEN_INVALID_OR_EXPIRED");
            throw new CustomAuthException(detail.reasonCode(), detail.title(), detail.message(), detail.nextStep());
        }

        // Mark token used
        emailToken.setUsedAt(Instant.now());
        emailTokenRepository.save(emailToken);

        // Mark user verified
        User user = userRepository.findById(emailToken.getUserId())
                .orElseThrow(() -> {
                    ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("TOKEN_INVALID_OR_EXPIRED");
                    return new CustomAuthException(detail.reasonCode(), detail.title(), detail.message(), detail.nextStep());
                });

        user.setEmailVerified(true);
        userRepository.save(user);

        auditService.logEvent(user.getId(), user.getEmail(), LoginEventType.EMAIL_VERIFIED,
                RiskLevel.LOW, 0, null, "EMAIL_VERIFIED", null, null);

        return GenericResponse.ok("Email verified successfully");
    }

    public GenericResponse resendVerification(String email, String demoSessionId, String clientIp) {
        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> optionalUser = userRepository.findByEmail(normalizedEmail);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (!user.isEmailVerified()) {
                String rawToken = CryptoUtil.generateRandomToken(32);
                String tokenHash = CryptoUtil.sha256(rawToken);
                Instant expiresAt = Instant.now().plus(Duration.ofHours(24));

                EmailToken emailToken = new EmailToken(user.getId(), TokenType.VERIFY, tokenHash, expiresAt);
                emailTokenRepository.save(emailToken);

                emailService.sendVerificationEmail(demoSessionId, normalizedEmail, rawToken);
            }
        }

        // Generic response
        return GenericResponse.ok();
    }

    public static class CustomAuthException extends RuntimeException {
        private final String reasonCode;
        private final String title;
        private final String message;
        private final String nextStep;

        public CustomAuthException(String reasonCode, String title, String message, String nextStep) {
            super(message);
            this.reasonCode = reasonCode;
            this.title = title;
            this.message = message;
            this.nextStep = nextStep;
        }

        public String getReasonCode() {
            return reasonCode;
        }

        public String getTitle() {
            return title;
        }

        public String getNextStep() {
            return nextStep;
        }
    }
}
