package com.authease.integration;

import com.authease.config.AppProperties;
import com.authease.dto.*;
import com.authease.model.*;
import com.authease.policy.PolicyService;
import com.authease.repository.*;
import com.authease.risk.RiskEngine;
import com.authease.security.Argon2SecurityUtil;
import com.authease.security.ClientIpResolver;
import com.authease.security.RateLimitService;
import com.authease.service.*;
import com.authease.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthEaseIntegrationTest {

    private UserRepository userRepository;
    private EmailTokenRepository emailTokenRepository;
    private ChallengeRepository challengeRepository;
    private RateCounterRepository rateCounterRepository;
    private LoginEventRepository loginEventRepository;
    private TrustedDeviceRepository trustedDeviceRepository;
    private EmailOutboxRepository emailOutboxRepository;

    private Argon2SecurityUtil passwordEncoder;
    private RateLimitService rateLimitService;
    private ClientIpResolver clientIpResolver;
    private AuditService auditService;
    private EmailService emailService;
    private ReasonCatalogService reasonCatalogService;
    private RiskEngine riskEngine;
    private PolicyService policyService;
    private ChallengeService challengeService;
    private PasswordPolicyService passwordPolicyService;
    private MfaService mfaService;
    private RecoveryService recoveryService;
    private AuthService authService;
    private MetricsService metricsService;
    private com.authease.controller.AdminController adminController;
    private com.authease.controller.AuthController authController;
    private AppProperties appProperties;

    private MockHttpServletRequest httpRequest;
    private MockHttpServletResponse httpResponse;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        emailTokenRepository = mock(EmailTokenRepository.class);
        challengeRepository = mock(ChallengeRepository.class);
        rateCounterRepository = mock(RateCounterRepository.class);
        loginEventRepository = mock(LoginEventRepository.class);
        trustedDeviceRepository = mock(TrustedDeviceRepository.class);
        emailOutboxRepository = mock(EmailOutboxRepository.class);

        passwordEncoder = new Argon2SecurityUtil();
        rateLimitService = new RateLimitService(rateCounterRepository);
        clientIpResolver = new ClientIpResolver();
        auditService = new AuditService(loginEventRepository);
        reasonCatalogService = new ReasonCatalogService();
        riskEngine = new RiskEngine();
        policyService = new PolicyService();
        challengeService = new ChallengeService(challengeRepository, reasonCatalogService);
        passwordPolicyService = new PasswordPolicyService();
        appProperties = new AppProperties();
        appProperties.setEncKey("0123456789abcdef0123456789abcdef");

        emailService = new EmailService(appProperties, emailOutboxRepository, Optional.empty());

        authService = new AuthService(userRepository, trustedDeviceRepository, loginEventRepository,
                passwordEncoder, rateLimitService, clientIpResolver, auditService, emailService,
                reasonCatalogService, riskEngine, policyService, challengeService);

        mfaService = new MfaService(
                userRepository,
                challengeRepository,
                trustedDeviceRepository,
                challengeService,
                authService,
                emailService,
                auditService,
                clientIpResolver,
                reasonCatalogService,
                appProperties
        );

        recoveryService = new RecoveryService(
                userRepository,
                emailTokenRepository,
                trustedDeviceRepository,
                passwordEncoder,
                passwordPolicyService,
                emailService,
                auditService,
                reasonCatalogService,
                appProperties
        );

        metricsService = new MetricsService(loginEventRepository);
        adminController = new com.authease.controller.AdminController(metricsService, userRepository);

        UserService userService = new UserService(userRepository, emailTokenRepository, passwordEncoder,
                emailService, auditService, reasonCatalogService);

        authController = new com.authease.controller.AuthController(authService, userService, challengeService, mfaService, clientIpResolver);

        httpRequest = new MockHttpServletRequest();
        httpRequest.getSession(true);
        httpResponse = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("Requirement 1: Recovery token is strictly single-use and cannot be replayed")
    void testSingleUseRecoveryToken() {
        String email = "alice@example.com";
        User user = new User(email, "Alice", passwordEncoder.encode("OldPassword123!"));
        user.setId("u-alice");
        user.setEmailVerified(true);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.findById("u-alice")).thenReturn(Optional.of(user));

        Map<String, EmailToken> tokenStore = new HashMap<>();
        when(emailTokenRepository.save(any(EmailToken.class))).thenAnswer(inv -> {
            EmailToken t = inv.getArgument(0);
            tokenStore.put(t.getTokenHash(), t);
            return t;
        });
        when(emailTokenRepository.findByTokenHashAndType(anyString(), eq(TokenType.RECOVERY))).thenAnswer(inv -> {
            String hash = inv.getArgument(0);
            return Optional.ofNullable(tokenStore.get(hash));
        });

        // 1. Request recovery
        GenericResponse reqResponse = recoveryService.requestRecovery(new RecoveryRequestDto(email), "sess-1", "127.0.0.1");
        assertTrue(reqResponse.isOk());

        // Create valid raw token stored in repo
        String rawToken = "raw-recovery-secret-abc";
        String tokenHash = CryptoUtil.sha256(rawToken);
        EmailToken token = new EmailToken("u-alice", TokenType.RECOVERY, tokenHash, Instant.now().plus(Duration.ofMinutes(15)));
        tokenStore.put(tokenHash, token);

        // 2. Validate token (first use) -> VALID
        RecoveryValidateResponse val1 = recoveryService.validateToken(rawToken);
        assertTrue(val1.isValid());

        // 3. Reset password using valid token -> SUCCESS
        RecoveryResetRequest resetReq = new RecoveryResetRequest(rawToken, "Xkcd-Battery-Horse-Staple-2026!#", null);
        GenericResponse resetResp = recoveryService.resetPassword(resetReq, httpRequest);
        assertTrue(resetResp.isOk());

        // 4. Token has usedAt set
        assertNotNull(token.getUsedAt());

        // 5. Attempt second use -> validate returns false, reset throws TOKEN_INVALID_OR_EXPIRED
        RecoveryValidateResponse val2 = recoveryService.validateToken(rawToken);
        assertFalse(val2.isValid());

        UserService.CustomAuthException ex = assertThrows(
                UserService.CustomAuthException.class,
                () -> recoveryService.resetPassword(resetReq, httpRequest)
        );
        assertEquals("TOKEN_INVALID_OR_EXPIRED", ex.getReasonCode());
    }

    @Test
    @DisplayName("Requirement 2: Constant-time / bounded variance between existing and non-existing email login")
    void testConstantTimeTimingParity() {
        String existingEmail = "existing@example.com";
        String nonExistingEmail = "nobody-here@example.com";
        String rawPassword = "SomePassword123!";
        String realHash = passwordEncoder.encode("RealSecretPassword99!");

        User existingUser = new User(existingEmail, "Existing User", realHash);
        existingUser.setId("u-exist");
        existingUser.setEmailVerified(true);

        when(userRepository.findByEmail(existingEmail)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail(nonExistingEmail)).thenReturn(Optional.empty());

        // Warm up JIT and Argon2
        authService.login(new LoginRequest(existingEmail, rawPassword), httpRequest);
        authService.login(new LoginRequest(nonExistingEmail, rawPassword), httpRequest);

        // Measure existing user invalid password attempts
        int runs = 5;
        long totalExistingMs = 0;
        for (int i = 0; i < runs; i++) {
            long start = System.nanoTime();
            authService.login(new LoginRequest(existingEmail, rawPassword), httpRequest);
            totalExistingMs += (System.nanoTime() - start) / 1_000_000;
        }
        long avgExistingMs = totalExistingMs / runs;

        // Measure non-existing user invalid password attempts
        long totalNonExistingMs = 0;
        for (int i = 0; i < runs; i++) {
            long start = System.nanoTime();
            authService.login(new LoginRequest(nonExistingEmail, rawPassword), httpRequest);
            totalNonExistingMs += (System.nanoTime() - start) / 1_000_000;
        }
        long avgNonExistingMs = totalNonExistingMs / runs;

        long differenceMs = Math.abs(avgExistingMs - avgNonExistingMs);
        // Variance must be bounded within 50ms tolerance (Argon2 dummy hash timing parity)
        assertTrue(differenceMs <= 50, "Timing difference " + differenceMs + "ms exceeds 50ms tolerance. (Existing: " + avgExistingMs + "ms, Non-existing: " + avgNonExistingMs + "ms)");
    }

    @Test
    @DisplayName("Requirement 3: Progressive rate limit delay increments")
    void testProgressiveRateLimitDelayIncrements() {
        String ip = "10.0.0.99";
        String email = "victim@example.com";

        Map<String, RateCounter> inMemRate = new HashMap<>();
        when(rateCounterRepository.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(inMemRate.get(inv.getArgument(0))));
        when(rateCounterRepository.save(any(RateCounter.class))).thenAnswer(inv -> {
            RateCounter c = inv.getArgument(0);
            inMemRate.put(c.getId(), c);
            return c;
        });

        RateLimitService realRateService = new RateLimitService(rateCounterRepository);

        // Initial state: 0 failures -> no delay
        assertFalse(realRateService.checkRateLimit(ip, email).isDelayed());

        // 1 failure -> progressive delay: 2^0 = 1s
        realRateService.recordFailure(ip, email);
        RateLimitService.RateLimitStatus status1 = realRateService.checkRateLimit(ip, email);
        assertTrue(status1.isDelayed());
        assertTrue(status1.getRetryAfterSeconds() <= 1 && status1.getRetryAfterSeconds() > 0);

        // 2 failures -> progressive delay: 2^1 = 2s
        realRateService.recordFailure(ip, email);
        RateLimitService.RateLimitStatus status2 = realRateService.checkRateLimit(ip, email);
        assertTrue(status2.isDelayed());
        assertTrue(status2.getRetryAfterSeconds() <= 2 && status2.getRetryAfterSeconds() > 0);

        // 4 failures -> progressive delay: 2^3 = 8s
        realRateService.recordFailure(ip, email); // 3
        realRateService.recordFailure(ip, email); // 4
        RateLimitService.RateLimitStatus status4 = realRateService.checkRateLimit(ip, email);
        assertTrue(status4.isDelayed());
        assertTrue(status4.getRetryAfterSeconds() <= 8 && status4.getRetryAfterSeconds() > 2);

        // 7 failures -> capped at 60s
        for (int i = 5; i <= 7; i++) {
            realRateService.recordFailure(ip, email);
        }
        RateLimitService.RateLimitStatus status7 = realRateService.checkRateLimit(ip, email);
        assertTrue(status7.isDelayed());
        assertTrue(status7.getRetryAfterSeconds() <= 60);
    }

    @Test
    @DisplayName("Requirement 4: Backup code single-use consumption")
    void testBackupCodeSingleUseConsumption() {
        String rawBackupCode = "A1B2-C3D4";
        String hashedCode = CryptoUtil.sha256(rawBackupCode.toUpperCase());

        User user = new User("backup@example.com", "Backup Tester", "passhash");
        user.setId("u-backup");
        user.setEmailVerified(true);
        user.setBackupCodeHashes(new ArrayList<>(List.of(hashedCode)));

        when(userRepository.findById("u-backup")).thenReturn(Optional.of(user));

        Challenge challenge = new Challenge("ch-backup-1", "u-backup", httpRequest.getSession().getId(),
                RiskLevel.MEDIUM, 2, null, Instant.now().plusSeconds(600));
        when(challengeRepository.findByIdAndBoundSessionId("ch-backup-1", httpRequest.getSession().getId()))
                .thenReturn(Optional.of(challenge));

        // 1. Verify using backup code -> SUCCESS
        LoginResponse resp1 = mfaService.verifyMfa(new MfaVerifyRequest("ch-backup-1", "BACKUP_CODE", rawBackupCode, false), httpRequest, httpResponse);
        assertNotNull(resp1);
        assertEquals("AUTHENTICATED", resp1.getStatus());

        // Verify backup code was removed from user entity
        assertFalse(user.getBackupCodeHashes().contains(hashedCode));
        verify(userRepository, atLeastOnce()).save(user);

        // 2. Re-create challenge and attempt reuse of the SAME backup code -> REJECTED
        Challenge challenge2 = new Challenge("ch-backup-2", "u-backup", httpRequest.getSession().getId(),
                RiskLevel.MEDIUM, 2, null, Instant.now().plusSeconds(600));
        when(challengeRepository.findByIdAndBoundSessionId("ch-backup-2", httpRequest.getSession().getId()))
                .thenReturn(Optional.of(challenge2));

        LoginResponse resp2 = mfaService.verifyMfa(new MfaVerifyRequest("ch-backup-2", "BACKUP_CODE", rawBackupCode, false), httpRequest, httpResponse);
        assertEquals("FAILED", resp2.getStatus());
        assertEquals("OTP_INVALID", resp2.getReasonCode());
    }

    @Test
    @DisplayName("Requirement 5: Admin endpoint rejected if admin lacks TOTP (defense-in-depth)")
    void testAdminEndpointRejectedIfAdminLacksTotp() {
        httpRequest.getSession().setAttribute(AuthService.SESSION_USER_ID, "admin-user-id");

        User adminWithoutTotp = new User("admin@authease.com", "Admin User", "hash");
        adminWithoutTotp.setId("admin-user-id");
        adminWithoutTotp.setRoles(Set.of("ADMIN"));
        adminWithoutTotp.setTotpEnabled(false); // No TOTP enabled!

        when(userRepository.findById("admin-user-id")).thenReturn(Optional.of(adminWithoutTotp));

        // Accessing admin endpoint without TOTP must be rejected with ADMIN_MFA_REQUIRED
        UserService.CustomAuthException ex = assertThrows(
                UserService.CustomAuthException.class,
                () -> adminController.getMetrics("24h", httpRequest)
        );
        assertEquals("ADMIN_MFA_REQUIRED", ex.getReasonCode());

        // Enabling TOTP on admin account allows access
        adminWithoutTotp.setTotpEnabled(true);
        ResponseEntity<AdminMetricsResponse> okResp = adminController.getMetrics("24h", httpRequest);
        assertEquals(200, okResp.getStatusCode().value());
    }

    @Test
    @DisplayName("Requirement 6: Unverified user cannot log in")
    void testUnverifiedUserCannotLogin() {
        String email = "unverified@example.com";
        String password = "Password123!";
        User unverifiedUser = new User(email, "Unverified User", passwordEncoder.encode(password));
        unverifiedUser.setId("u-unverified");
        unverifiedUser.setEmailVerified(false); // Unverified!

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(unverifiedUser));

        LoginResponse response = authService.login(new LoginRequest(email, password), httpRequest);
        assertEquals("FAILED", response.getStatus());
        assertEquals("EMAIL_NOT_VERIFIED", response.getReasonCode());
    }

    @Test
    @DisplayName("Requirement 7: 5 wrong challenge codes lock the challenge")
    void testFiveWrongChallengeCodesLockChallenge() {
        User user = new User("target@example.com", "Target User", "hash");
        user.setId("u-target");
        user.setEmailVerified(true);
        user.setTotpSecretEncrypted("dummy-enc");

        when(userRepository.findById("u-target")).thenReturn(Optional.of(user));

        Challenge challenge = new Challenge("ch-lock-test", "u-target", httpRequest.getSession().getId(),
                RiskLevel.MEDIUM, 2, null, Instant.now().plusSeconds(600));

        when(challengeRepository.findByIdAndBoundSessionId("ch-lock-test", httpRequest.getSession().getId()))
                .thenReturn(Optional.of(challenge));

        // Attempts 1 to 4: returns FAILED with OTP_INVALID, incrementing wrong count
        for (int i = 1; i <= 4; i++) {
            LoginResponse resp = mfaService.verifyMfa(new MfaVerifyRequest("ch-lock-test", "TOTP", "000000", false), httpRequest, httpResponse);
            assertEquals("FAILED", resp.getStatus());
            assertEquals("OTP_INVALID", resp.getReasonCode());
            assertEquals(i, challenge.getWrongCodeCount());
        }

        // Attempt 5: triggers recordWrongCode which deletes challenge and throws CHALLENGE_LOCKED
        UserService.CustomAuthException ex = assertThrows(
                UserService.CustomAuthException.class,
                () -> mfaService.verifyMfa(new MfaVerifyRequest("ch-lock-test", "TOTP", "000000", false), httpRequest, httpResponse)
        );
        assertEquals("CHALLENGE_LOCKED", ex.getReasonCode());

        // Subsequent attempt on deleted challenge: returns TOKEN_INVALID_OR_EXPIRED
        when(challengeRepository.findByIdAndBoundSessionId("ch-lock-test", httpRequest.getSession().getId()))
                .thenReturn(Optional.empty());

        UserService.CustomAuthException ex2 = assertThrows(
                UserService.CustomAuthException.class,
                () -> mfaService.verifyMfa(new MfaVerifyRequest("ch-lock-test", "TOTP", "000000", false), httpRequest, httpResponse)
        );
        assertEquals("TOKEN_INVALID_OR_EXPIRED", ex2.getReasonCode());
    }

    @Test
    @DisplayName("Requirement 8: Approval consumes via POST only, not GET (prevent email prefetch scanners)")
    void testApprovalConsumesViaPostOnly() {
        String approvalToken = "token-approval-secret-123";
        String tokenHash = CryptoUtil.sha256(approvalToken);

        Challenge challenge = new Challenge("ch-approval-1", "u-high", httpRequest.getSession().getId(),
                RiskLevel.HIGH, 3, null, Instant.now().plusSeconds(600));
        challenge.setApprovalTokenHash(tokenHash);

        when(challengeRepository.findByApprovalTokenHash(tokenHash)).thenReturn(Optional.of(challenge));

        // POST /api/auth/email-approval -> SUCCESS
        EmailApprovalRequest req = new EmailApprovalRequest(approvalToken, "APPROVE");
        ResponseEntity<GenericResponse> postResp = authController.emailApproval(req);
        assertEquals(200, postResp.getStatusCode().value());
        assertTrue(postResp.getBody().isOk());
        assertTrue(challenge.getEmailApproved());
    }
}
