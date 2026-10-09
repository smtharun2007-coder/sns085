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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MfaServiceTest {

    private UserRepository userRepository;
    private ChallengeRepository challengeRepository;
    private TrustedDeviceRepository trustedDeviceRepository;
    private ChallengeService challengeService;
    private AuthService authService;
    private EmailService emailService;
    private AuditService auditService;
    private ClientIpResolver clientIpResolver;
    private ReasonCatalogService reasonCatalogService;
    private AppProperties appProperties;
    private MfaService mfaService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        challengeRepository = mock(ChallengeRepository.class);
        trustedDeviceRepository = mock(TrustedDeviceRepository.class);
        challengeService = mock(ChallengeService.class);
        authService = mock(AuthService.class);
        emailService = mock(EmailService.class);
        auditService = mock(AuditService.class);
        clientIpResolver = mock(ClientIpResolver.class);
        reasonCatalogService = new ReasonCatalogService();
        appProperties = mock(AppProperties.class);
        when(appProperties.getEncKey()).thenReturn("0123456789abcdef0123456789abcdef");

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
    }

    @Test
    void testSendEmailOtp_GeneratesCodeAndSendsEmail() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true);

        Challenge challenge = new Challenge("c1", "u1", request.getSession().getId(), RiskLevel.MEDIUM, 2, null, Instant.now().plusSeconds(600));
        User user = new User("alice@example.com", "Alice", "hash");
        user.setId("u1");

        when(challengeService.findValidChallenge("c1", request.getSession().getId())).thenReturn(Optional.of(challenge));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        GenericResponse resp = mfaService.sendEmailOtp("c1", request);
        assertEquals("OK", resp.getStatus());

        assertNotNull(challenge.getEmailOtpHash());
        assertNotNull(challenge.getEmailOtpExpiresAt());
        verify(emailService).sendEmailOtp(eq(request.getSession().getId()), eq("alice@example.com"), anyString());
        verify(challengeRepository).save(challenge);
    }

    @Test
    void testVerifyMfa_EmailOtp_ValidCode_Authenticates() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.getSession(true);

        String rawCode = "123456";
        String codeHash = CryptoUtil.sha256(rawCode);

        Challenge challenge = new Challenge("c1", "u1", request.getSession().getId(), RiskLevel.MEDIUM, 2, null, Instant.now().plusSeconds(600));
        challenge.setEmailOtpHash(codeHash);
        challenge.setEmailOtpExpiresAt(Instant.now().plusSeconds(300));

        User user = new User("alice@example.com", "Alice", "hash");
        user.setId("u1");

        when(challengeService.findValidChallenge("c1", request.getSession().getId())).thenReturn(Optional.of(challenge));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        MfaVerifyRequest verifyReq = new MfaVerifyRequest("c1", "EMAIL_OTP", rawCode, false);
        LoginResponse loginResp = mfaService.verifyMfa(verifyReq, request, response);

        assertEquals("AUTHENTICATED", loginResp.getStatus());
        verify(authService).createAuthenticatedSession(user, request);
        verify(challengeRepository).delete(challenge);
    }

    @Test
    void testVerifyMfa_BackupCode_SingleUse() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.getSession(true);

        String rawBackupCode = "A1B2-C3D4";
        String backupHash = CryptoUtil.sha256(rawBackupCode.toUpperCase());

        Challenge challenge = new Challenge("c1", "u1", request.getSession().getId(), RiskLevel.MEDIUM, 2, null, Instant.now().plusSeconds(600));

        User user = new User("alice@example.com", "Alice", "hash");
        user.setId("u1");
        List<String> codes = new ArrayList<>(List.of(backupHash));
        user.setBackupCodeHashes(codes);

        when(challengeService.findValidChallenge("c1", request.getSession().getId())).thenReturn(Optional.of(challenge));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        MfaVerifyRequest verifyReq = new MfaVerifyRequest("c1", "BACKUP_CODE", rawBackupCode, false);
        LoginResponse loginResp = mfaService.verifyMfa(verifyReq, request, response);

        assertEquals("AUTHENTICATED", loginResp.getStatus());
        // Verify backup code was consumed
        assertFalse(user.getBackupCodeHashes().contains(backupHash));
        verify(userRepository).save(user);
    }

    @Test
    void testVerifyMfa_RememberDevice_SetsCookieAndPersistsTrust() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.getSession(true);

        String rawCode = "123456";
        String codeHash = CryptoUtil.sha256(rawCode);

        Challenge challenge = new Challenge("c1", "u1", request.getSession().getId(), RiskLevel.MEDIUM, 2, null, Instant.now().plusSeconds(600));
        challenge.setEmailOtpHash(codeHash);
        challenge.setEmailOtpExpiresAt(Instant.now().plusSeconds(300));

        User user = new User("alice@example.com", "Alice", "hash");
        user.setId("u1");

        when(challengeService.findValidChallenge("c1", request.getSession().getId())).thenReturn(Optional.of(challenge));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        MfaVerifyRequest verifyReq = new MfaVerifyRequest("c1", "EMAIL_OTP", rawCode, true); // rememberDevice=true
        mfaService.verifyMfa(verifyReq, request, response);

        verify(trustedDeviceRepository).save(any(TrustedDevice.class));
        assertNotNull(response.getCookie("ae_device"));
        assertTrue(response.getCookie("ae_device").isHttpOnly());
    }

    @Test
    void testVerifyMfa_HighRisk_AwaitingApproval_ReturnsPendingApproval() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.getSession(true);

        String rawCode = "123456";
        String codeHash = CryptoUtil.sha256(rawCode);

        Challenge challenge = new Challenge("c1", "u1", request.getSession().getId(), RiskLevel.HIGH, 3, null, Instant.now().plusSeconds(600));
        challenge.setEmailOtpHash(codeHash);
        challenge.setEmailOtpExpiresAt(Instant.now().plusSeconds(300));
        challenge.setEmailApproved(false); // Not approved yet

        User user = new User("alice@example.com", "Alice", "hash");
        user.setId("u1");

        when(challengeService.findValidChallenge("c1", request.getSession().getId())).thenReturn(Optional.of(challenge));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        MfaVerifyRequest verifyReq = new MfaVerifyRequest("c1", "EMAIL_OTP", rawCode, false);
        LoginResponse loginResp = mfaService.verifyMfa(verifyReq, request, response);

        assertEquals("MFA_REQUIRED", loginResp.getStatus());
        assertEquals("EMAIL_APPROVAL_PENDING", loginResp.getReasonCode());
        assertEquals(2, loginResp.getStep().getCurrent());
        assertEquals(3, loginResp.getStep().getTotal());
        assertTrue(loginResp.getNeedsEmailApproval());
    }

    @Test
    void testEmailApproval_Approve_SetsApprovedTrue() {
        String rawToken = "approval-token-xyz";
        String tokenHash = CryptoUtil.sha256(rawToken);

        Challenge challenge = new Challenge("c1", "u1", "sess1", RiskLevel.HIGH, 3, null, Instant.now().plusSeconds(600));
        challenge.setApprovalTokenHash(tokenHash);

        when(challengeRepository.findByApprovalTokenHash(tokenHash)).thenReturn(Optional.of(challenge));

        EmailApprovalRequest req = new EmailApprovalRequest(rawToken, "APPROVE");
        GenericResponse resp = mfaService.emailApproval(req);

        assertEquals("OK", resp.getStatus());
        assertTrue(challenge.getEmailApproved());
        assertNull(challenge.getApprovalTokenHash()); // Single use cleared
        verify(challengeRepository).save(challenge);
    }

    @Test
    void testEmailApproval_Deny_DeletesChallengeAndLogsSuspicious() {
        String rawToken = "approval-token-xyz";
        String tokenHash = CryptoUtil.sha256(rawToken);

        Challenge challenge = new Challenge("c1", "u1", "sess1", RiskLevel.HIGH, 3, null, Instant.now().plusSeconds(600));
        challenge.setApprovalTokenHash(tokenHash);

        User user = new User("alice@example.com", "Alice", "hash");
        user.setId("u1");

        when(challengeRepository.findByApprovalTokenHash(tokenHash)).thenReturn(Optional.of(challenge));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        EmailApprovalRequest req = new EmailApprovalRequest(rawToken, "DENY");
        GenericResponse resp = mfaService.emailApproval(req);

        assertEquals("OK", resp.getStatus());
        verify(challengeRepository).delete(challenge);
        verify(auditService).logEvent(eq("u1"), eq("alice@example.com"), eq(LoginEventType.SUSPICIOUS_DETECTED),
                eq(RiskLevel.HIGH), any(), any(), eq("EMAIL_APPROVAL_DENIED"), any(), any());
        verify(emailService).sendEmail(any(), eq("alice@example.com"), anyString(), anyString());
    }

    @Test
    void testConfirmTotp_ValidCode_EnablesTotpAndReturnsBackupCodes() {
        String secret = TotpUtil.generateSecret(32);
        long currentStep = System.currentTimeMillis() / 1000 / 30;
        String validCode = TotpUtil.generateCode(secret, currentStep);

        User user = new User("alice@example.com", "Alice", "hash");
        user.setId("u1");

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        BackupCodesResponse resp = mfaService.confirmTotp("u1", secret, validCode);
        assertNotNull(resp.getBackupCodes());
        assertEquals(10, resp.getBackupCodes().size());
        assertTrue(user.isTotpEnabled());
        assertNotNull(user.getTotpSecretEncrypted());
        assertEquals(10, user.getBackupCodeHashes().size());
        verify(userRepository).save(user);
    }

    @Test
    void testVerifyMfa_DemoModeBypassCode_Succeeds() {
        when(appProperties.isDemoMode()).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.getSession(true);

        User user = new User("admin@authease.demo", "Demo Admin", "hash");
        user.setId("admin-1");
        user.setTotpEnabled(true);

        Challenge challenge = new Challenge("ch-admin-1", "admin-1", request.getSession().getId(), RiskLevel.MEDIUM, 2, null, Instant.now().plusSeconds(300));

        when(challengeService.findValidChallenge("ch-admin-1", request.getSession().getId())).thenReturn(Optional.of(challenge));
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(user));

        MfaVerifyRequest verifyReq = new MfaVerifyRequest("ch-admin-1", "TOTP", "123456", false);
        LoginResponse resp = mfaService.verifyMfa(verifyReq, request, response);

        assertEquals("AUTHENTICATED", resp.getStatus());
        verify(challengeRepository).delete(challenge);
    }
}
