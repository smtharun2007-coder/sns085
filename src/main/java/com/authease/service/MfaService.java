package com.authease.service;

import com.authease.config.AppProperties;
import com.authease.dto.*;
import com.authease.model.*;
import com.authease.repository.ChallengeRepository;
import com.authease.repository.TrustedDeviceRepository;
import com.authease.repository.UserRepository;
import com.authease.security.ClientIpResolver;
import com.authease.util.CryptoUtil;
import com.authease.util.TotpUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MfaService {

    private final UserRepository userRepository;
    private final ChallengeRepository challengeRepository;
    private final TrustedDeviceRepository trustedDeviceRepository;
    private final ChallengeService challengeService;
    private final AuthService authService;
    private final EmailService emailService;
    private final AuditService auditService;
    private final ClientIpResolver clientIpResolver;
    private final ReasonCatalogService reasonCatalogService;
    private final AppProperties appProperties;

    public MfaService(UserRepository userRepository,
                      ChallengeRepository challengeRepository,
                      TrustedDeviceRepository trustedDeviceRepository,
                      ChallengeService challengeService,
                      AuthService authService,
                      EmailService emailService,
                      AuditService auditService,
                      ClientIpResolver clientIpResolver,
                      ReasonCatalogService reasonCatalogService,
                      AppProperties appProperties) {
        this.userRepository = userRepository;
        this.challengeRepository = challengeRepository;
        this.trustedDeviceRepository = trustedDeviceRepository;
        this.challengeService = challengeService;
        this.authService = authService;
        this.emailService = emailService;
        this.auditService = auditService;
        this.clientIpResolver = clientIpResolver;
        this.reasonCatalogService = reasonCatalogService;
        this.appProperties = appProperties;
    }

    public GenericResponse sendEmailOtp(String challengeId, HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        String boundSessionId = session != null ? session.getId() : "";

        Challenge challenge = challengeService.findValidChallenge(challengeId, boundSessionId)
                .orElseThrow(() -> {
                    ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("TOKEN_INVALID_OR_EXPIRED");
                    return new UserService.CustomAuthException(detail.reasonCode(), detail.title(), detail.message(), detail.nextStep());
                });

        User user = userRepository.findById(challenge.getUserId())
                .orElseThrow(() -> {
                    ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("TOKEN_INVALID_OR_EXPIRED");
                    return new UserService.CustomAuthException(detail.reasonCode(), detail.title(), detail.message(), detail.nextStep());
                });

        String code = CryptoUtil.generateSixDigitCode();
        challenge.setEmailOtpHash(CryptoUtil.sha256(code));
        challenge.setEmailOtpExpiresAt(Instant.now().plus(Duration.ofMinutes(5)));
        challengeRepository.save(challenge);

        emailService.sendEmailOtp(boundSessionId, user.getEmail(), code);

        return GenericResponse.ok("Code sent to email");
    }

    public LoginResponse verifyMfa(MfaVerifyRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        HttpSession session = httpRequest.getSession(false);
        String boundSessionId = session != null ? session.getId() : "";
        String clientIp = clientIpResolver.resolveClientIp(httpRequest);
        String ipPrefixHash = clientIpResolver.getNetworkPrefixHash(clientIp);

        Challenge challenge = challengeService.findValidChallenge(request.getChallengeId(), boundSessionId)
                .orElseThrow(() -> {
                    ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("TOKEN_INVALID_OR_EXPIRED");
                    return new UserService.CustomAuthException(detail.reasonCode(), detail.title(), detail.message(), detail.nextStep());
                });

        User user = userRepository.findById(challenge.getUserId())
                .orElseThrow(() -> {
                    ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("TOKEN_INVALID_OR_EXPIRED");
                    return new UserService.CustomAuthException(detail.reasonCode(), detail.title(), detail.message(), detail.nextStep());
                });

        String method = request.getMethod().trim().toUpperCase();
        String rawCode = request.getCode().trim();
        boolean codeValid = false;

        if (appProperties.isDemoMode() && ("123456".equals(rawCode) || "000000".equals(rawCode))) {
            codeValid = true;
        } else if ("TOTP".equals(method)) {
            if (user.isTotpEnabled() && user.getTotpSecretEncrypted() != null) {
                String plainSecret = CryptoUtil.decryptAesGcm(user.getTotpSecretEncrypted(), appProperties.getEncKey());
                Long matchingStep = TotpUtil.validateCode(plainSecret, rawCode, challenge.getLastUsedTotpTimeStep());
                if (matchingStep != null) {
                    codeValid = true;
                    challenge.setLastUsedTotpTimeStep(matchingStep);
                    challengeRepository.save(challenge);
                }
            }
        } else if ("EMAIL_OTP".equals(method)) {
            if (challenge.getEmailOtpHash() != null) {
                if (challenge.getEmailOtpExpiresAt() != null && challenge.getEmailOtpExpiresAt().isBefore(Instant.now())) {
                    ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("OTP_EXPIRED");
                    return LoginResponse.failed(detail.reasonCode(), detail.title(), detail.message(), detail.nextStep());
                }
                String codeHash = CryptoUtil.sha256(rawCode);
                if (CryptoUtil.constantTimeEquals(challenge.getEmailOtpHash(), codeHash)) {
                    codeValid = true;
                    // single-use
                    challenge.setEmailOtpHash(null);
                    challengeRepository.save(challenge);
                }
            }
        } else if ("BACKUP_CODE".equals(method)) {
            String codeHashUpper = CryptoUtil.sha256(rawCode.toUpperCase());
            String codeHashExact = CryptoUtil.sha256(rawCode);
            List<String> userBackupHashes = user.getBackupCodeHashes();
            if (userBackupHashes != null) {
                if (userBackupHashes.contains(codeHashUpper) || userBackupHashes.contains(codeHashExact)) {
                    codeValid = true;
                    userBackupHashes.remove(codeHashUpper);
                    userBackupHashes.remove(codeHashExact);
                    user.setBackupCodeHashes(userBackupHashes);
                    userRepository.save(user);
                }
            }
        }

        if (!codeValid) {
            challengeService.recordWrongCode(challenge);
            auditService.logEvent(user.getId(), user.getEmail(), LoginEventType.MFA_FAILURE,
                    challenge.getLevel(), null, List.of(method), "OTP_INVALID", ipPrefixHash, null);

            ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("OTP_INVALID");
            return LoginResponse.failed(detail.reasonCode(), detail.title(), detail.message(), detail.nextStep());
        }

        // Code was valid!
        auditService.logEvent(user.getId(), user.getEmail(), LoginEventType.MFA_SUCCESS,
                challenge.getLevel(), null, List.of(method), "MFA_SUCCESS", ipPrefixHash, null);

        // Check if High risk extra steps are required (email approval & 30s delay)
        if (challenge.getLevel() == RiskLevel.HIGH) {
            if (!Boolean.TRUE.equals(challenge.getEmailApproved())) {
                ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("EMAIL_APPROVAL_PENDING");
                return LoginResponse.mfaRequired(
                        challenge.getId(),
                        challenge.getLevel().name(),
                        List.of(method),
                        true,
                        2, // step 2 of 3 completed
                        3,
                        detail.reasonCode(),
                        detail.title(),
                        detail.message(),
                        detail.nextStep()
                );
            }
            if (challenge.getMinReleaseAt() != null && challenge.getMinReleaseAt().isAfter(Instant.now())) {
                long waitSeconds = Duration.between(Instant.now(), challenge.getMinReleaseAt()).toSeconds();
                ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("RATE_DELAY");
                return LoginResponse.delayed(Math.max(1, waitSeconds), detail.reasonCode(),
                        detail.title(), detail.message(), detail.nextStep());
            }
        }

        // All steps completed! Establish authenticated session
        authService.createAuthenticatedSession(user, httpRequest);

        // Issue trusted device cookie if requested
        if (Boolean.TRUE.equals(request.getRememberDevice())) {
            issueTrustedDeviceCookie(user.getId(), httpRequest, httpResponse);
        }

        // Delete completed challenge
        challengeRepository.delete(challenge);

        auditService.logEvent(user.getId(), user.getEmail(), LoginEventType.LOGIN_SUCCESS,
                challenge.getLevel(), null, List.of("MFA_COMPLETED"), "LOGIN_SUCCESS", ipPrefixHash, null);

        return LoginResponse.authenticated();
    }

    public GenericResponse emailApproval(EmailApprovalRequest request) {
        String tokenHash = CryptoUtil.sha256(request.getToken().trim());
        Optional<Challenge> optionalChallenge = challengeRepository.findByApprovalTokenHash(tokenHash);

        if (optionalChallenge.isEmpty()) {
            ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("TOKEN_INVALID_OR_EXPIRED");
            throw new UserService.CustomAuthException(detail.reasonCode(), detail.title(), detail.message(), detail.nextStep());
        }

        Challenge challenge = optionalChallenge.get();
        if (challenge.getExpiresAt().isBefore(Instant.now())) {
            challengeRepository.delete(challenge);
            ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("TOKEN_INVALID_OR_EXPIRED");
            throw new UserService.CustomAuthException(detail.reasonCode(), detail.title(), detail.message(), detail.nextStep());
        }

        User user = userRepository.findById(challenge.getUserId()).orElse(null);

        if ("APPROVE".equalsIgnoreCase(request.getDecision())) {
            challenge.setEmailApproved(true);
            challenge.setApprovalTokenHash(null); // Single use
            challengeRepository.save(challenge);
            return GenericResponse.ok("Sign-in approved successfully.");
        } else {
            // DENY: ends challenge, logs SUSPICIOUS_DETECTED, notifies owner
            challengeRepository.delete(challenge);
            if (user != null) {
                auditService.logEvent(user.getId(), user.getEmail(), LoginEventType.SUSPICIOUS_DETECTED,
                        RiskLevel.HIGH, null, List.of("EMAIL_APPROVAL_DENIED"), "EMAIL_APPROVAL_DENIED", null, null);

                emailService.sendEmail(challenge.getBoundSessionId(), user.getEmail(),
                        "Security Warning: Sign-in was denied",
                        "Someone attempted to sign in to your AuthEase account, and the request was denied.\n"
                                + "If this was not you, we strongly recommend you change your password immediately.");
            }
            return GenericResponse.ok("Sign-in was denied. Your account is protected.");
        }
    }

    public TotpSetupResponse setupTotp(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserService.CustomAuthException("UNAUTHORIZED", "Sign in required", "Please sign in.", "Sign in first."));

        String secret = TotpUtil.generateSecret(32);
        String otpAuthUrl = TotpUtil.buildOtpAuthUrl(user.getEmail(), secret);
        String qrDataUrl = TotpUtil.generateQrDataUrl(otpAuthUrl);

        return new TotpSetupResponse(qrDataUrl, secret);
    }

    public BackupCodesResponse confirmTotp(String userId, String pendingSecret, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserService.CustomAuthException("UNAUTHORIZED", "Sign in required", "Please sign in.", "Sign in first."));

        Long match = TotpUtil.validateCode(pendingSecret, code.trim(), null);
        if (match == null) {
            ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("OTP_INVALID");
            throw new UserService.CustomAuthException(detail.reasonCode(), detail.title(), detail.message(), detail.nextStep());
        }

        String encryptedSecret = CryptoUtil.encryptAesGcm(pendingSecret, appProperties.getEncKey());
        user.setTotpSecretEncrypted(encryptedSecret);
        user.setTotpEnabled(true);

        List<String> rawBackupCodes = generateBackupCodesList(10);
        List<String> hashedCodes = new ArrayList<>();
        for (String c : rawBackupCodes) {
            hashedCodes.add(CryptoUtil.sha256(c));
        }
        user.setBackupCodeHashes(hashedCodes);
        userRepository.save(user);

        return new BackupCodesResponse(rawBackupCodes);
    }

    public BackupCodesResponse regenerateBackupCodes(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserService.CustomAuthException("UNAUTHORIZED", "Sign in required", "Please sign in.", "Sign in first."));

        if (!user.isTotpEnabled()) {
            ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("MFA_NOT_SET_UP");
            throw new UserService.CustomAuthException(detail.reasonCode(), detail.title(), detail.message(), detail.nextStep());
        }

        List<String> rawBackupCodes = generateBackupCodesList(10);
        List<String> hashedCodes = new ArrayList<>();
        for (String c : rawBackupCodes) {
            hashedCodes.add(CryptoUtil.sha256(c));
        }
        user.setBackupCodeHashes(hashedCodes);
        userRepository.save(user);

        return new BackupCodesResponse(rawBackupCodes);
    }

    private void issueTrustedDeviceCookie(String userId, HttpServletRequest request, HttpServletResponse response) {
        String rawToken = CryptoUtil.generateRandom256BitHex();
        String tokenHash = CryptoUtil.sha256(rawToken);

        String userAgent = request.getHeader("User-Agent");
        String label = (userAgent != null && !userAgent.isBlank())
                ? userAgent.substring(0, Math.min(60, userAgent.length()))
                : "Trusted Browser";

        Instant expiresAt = Instant.now().plus(Duration.ofDays(30));
        TrustedDevice device = new TrustedDevice(userId, tokenHash, label, expiresAt);
        trustedDeviceRepository.save(device);

        Cookie cookie = new Cookie(AuthService.DEVICE_COOKIE_NAME, rawToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setPath("/");
        cookie.setMaxAge((int) Duration.ofDays(30).toSeconds());
        // SameSite=Lax
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private List<String> generateBackupCodesList(int count) {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // 8 alphanumeric characters formatted e.g. "a1b2-c3d4"
            String raw = CryptoUtil.generateRandomToken(4);
            codes.add(raw.substring(0, 4) + "-" + raw.substring(4, 8));
        }
        return codes;
    }
}
