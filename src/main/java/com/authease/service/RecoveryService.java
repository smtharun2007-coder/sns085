package com.authease.service;

import com.authease.config.AppProperties;
import com.authease.dto.*;
import com.authease.model.EmailToken;
import com.authease.model.LoginEventType;
import com.authease.model.TokenType;
import com.authease.model.User;
import com.authease.repository.EmailTokenRepository;
import com.authease.repository.TrustedDeviceRepository;
import com.authease.repository.UserRepository;
import com.authease.security.Argon2SecurityUtil;
import com.authease.util.CryptoUtil;
import com.authease.util.TotpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class RecoveryService {

    private final UserRepository userRepository;
    private final EmailTokenRepository emailTokenRepository;
    private final TrustedDeviceRepository trustedDeviceRepository;
    private final Argon2SecurityUtil passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final EmailService emailService;
    private final AuditService auditService;
    private final ReasonCatalogService reasonCatalogService;
    private final AppProperties appProperties;

    public RecoveryService(UserRepository userRepository,
                           EmailTokenRepository emailTokenRepository,
                           TrustedDeviceRepository trustedDeviceRepository,
                           Argon2SecurityUtil passwordEncoder,
                           PasswordPolicyService passwordPolicyService,
                           EmailService emailService,
                           AuditService auditService,
                           ReasonCatalogService reasonCatalogService,
                           AppProperties appProperties) {
        this.userRepository = userRepository;
        this.emailTokenRepository = emailTokenRepository;
        this.trustedDeviceRepository = trustedDeviceRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyService = passwordPolicyService;
        this.emailService = emailService;
        this.auditService = auditService;
        this.reasonCatalogService = reasonCatalogService;
        this.appProperties = appProperties;
    }

    public GenericResponse requestRecovery(RecoveryRequestDto request, String demoSessionId, String clientIp) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        Optional<User> optionalUser = userRepository.findByEmail(normalizedEmail);

        if (optionalUser.isPresent() && optionalUser.get().isEmailVerified()) {
            User user = optionalUser.get();
            String rawToken = CryptoUtil.generateRandomToken(32);
            String tokenHash = CryptoUtil.sha256(rawToken);
            Instant expiresAt = Instant.now().plus(Duration.ofMinutes(15)); // 15 min TTL

            EmailToken token = new EmailToken(user.getId(), TokenType.RECOVERY, tokenHash, expiresAt);
            emailTokenRepository.save(token);

            String link = appProperties.getBaseUrl() + "/reset-password.html?token=" + rawToken;
            String subject = "AuthEase Password Reset Request";
            String body = "Hello " + user.getDisplayName() + ",\n\n"
                    + "We received a request to reset your AuthEase account password.\n"
                    + "Open this link within 15 minutes to set a new password:\n"
                    + link + "\n\n"
                    + "If you did not request this, you can safely ignore this email. Your password remains unchanged.";

            emailService.sendEmail(demoSessionId, user.getEmail(), subject, body);
            auditService.logEvent(user.getId(), user.getEmail(), LoginEventType.RECOVERY_REQUESTED,
                    null, null, null, "RECOVERY_SENT", null, null);
        } else {
            // Timing defense for unknown or unverified emails
            passwordEncoder.verifyAgainstDummyHash("dummy-timing-defense");
        }

        // Generic response to prevent enumeration
        ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("RECOVERY_SENT");
        return GenericResponse.ok(detail.message());
    }

    public RecoveryValidateResponse validateToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return new RecoveryValidateResponse(false, false);
        }

        String tokenHash = CryptoUtil.sha256(rawToken.trim());
        Optional<EmailToken> optional = emailTokenRepository.findByTokenHashAndType(tokenHash, TokenType.RECOVERY);

        if (optional.isEmpty()) {
            return new RecoveryValidateResponse(false, false);
        }

        EmailToken token = optional.get();
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            return new RecoveryValidateResponse(false, false);
        }

        User user = userRepository.findById(token.getUserId()).orElse(null);
        if (user == null || !user.isEmailVerified()) {
            return new RecoveryValidateResponse(false, false);
        }

        return new RecoveryValidateResponse(true, user.isTotpEnabled());
    }

    public GenericResponse resetPassword(RecoveryResetRequest request, HttpServletRequest httpRequest) {
        String tokenHash = CryptoUtil.sha256(request.getToken().trim());
        Optional<EmailToken> optionalToken = emailTokenRepository.findByTokenHashAndType(tokenHash, TokenType.RECOVERY);

        if (optionalToken.isEmpty()) {
            ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("TOKEN_INVALID_OR_EXPIRED");
            throw new UserService.CustomAuthException(detail.reasonCode(), detail.title(), detail.message(), detail.nextStep());
        }

        EmailToken token = optionalToken.get();
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("TOKEN_INVALID_OR_EXPIRED");
            throw new UserService.CustomAuthException(detail.reasonCode(), detail.title(), detail.message(), detail.nextStep());
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> {
                    ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("TOKEN_INVALID_OR_EXPIRED");
                    return new UserService.CustomAuthException(detail.reasonCode(), detail.title(), detail.message(), detail.nextStep());
                });

        // 1. Validate password policy (NIST SP 800-63B + HIBP check)
        passwordPolicyService.validatePasswordOrThrow(request.getNewPassword());

        // 2. Second factor verification if TOTP enabled (Recovery must not bypass MFA!)
        if (user.isTotpEnabled()) {
            String code = request.getSecondFactorCode();
            if (code == null || code.isBlank()) {
                ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("OTP_INVALID");
                throw new UserService.CustomAuthException(detail.reasonCode(),
                        "Second factor code required",
                        "Because two-factor authentication is enabled on your account, please enter a code from your authenticator app or an emergency backup code.",
                        "Check your authenticator app or enter a saved emergency code.");
            }

            boolean secondFactorValid = false;
            String rawCode = code.trim();

            // Try TOTP
            if (user.getTotpSecretEncrypted() != null) {
                try {
                    String plainSecret = CryptoUtil.decryptAesGcm(user.getTotpSecretEncrypted(), appProperties.getEncKey());
                    Long matchStep = TotpUtil.validateCode(plainSecret, rawCode, null);
                    if (matchStep != null) {
                        secondFactorValid = true;
                    }
                } catch (Exception ignored) {}
            }

            // Try Backup Code
            if (!secondFactorValid) {
                String hashUpper = CryptoUtil.sha256(rawCode.toUpperCase());
                String hashExact = CryptoUtil.sha256(rawCode);
                List<String> backupHashes = user.getBackupCodeHashes();
                if (backupHashes != null && (backupHashes.contains(hashUpper) || backupHashes.contains(hashExact))) {
                    secondFactorValid = true;
                    backupHashes.remove(hashUpper);
                    backupHashes.remove(hashExact);
                    user.setBackupCodeHashes(backupHashes);
                }
            }

            if (!secondFactorValid) {
                ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("OTP_INVALID");
                throw new UserService.CustomAuthException(detail.reasonCode(), detail.title(), detail.message(), detail.nextStep());
            }
        }

        // 3. Mark recovery token used
        token.setUsedAt(Instant.now());
        emailTokenRepository.save(token);

        // 4. Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 5. Invalidate all sessions & revoke all trusted devices
        HttpSession currentSession = httpRequest.getSession(false);
        if (currentSession != null) {
            currentSession.invalidate();
        }
        trustedDeviceRepository.deleteByUserId(user.getId());

        // 6. Log audit event and send email alert
        auditService.logEvent(user.getId(), user.getEmail(), LoginEventType.PASSWORD_CHANGED,
                null, null, null, "PASSWORD_CHANGED", null, null);
        auditService.logEvent(user.getId(), user.getEmail(), LoginEventType.RECOVERY_COMPLETED,
                null, null, null, "RECOVERY_COMPLETED", null, null);

        emailService.sendPasswordChangedAlert(
                httpRequest.getSession(true).getId(),
                user.getEmail()
        );

        return GenericResponse.ok("Password has been reset successfully. Please sign in with your new password.");
    }
}
