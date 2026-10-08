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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecoveryServiceTest {

    private UserRepository userRepository;
    private EmailTokenRepository emailTokenRepository;
    private TrustedDeviceRepository trustedDeviceRepository;
    private Argon2SecurityUtil passwordEncoder;
    private PasswordPolicyService passwordPolicyService;
    private EmailService emailService;
    private AuditService auditService;
    private ReasonCatalogService reasonCatalogService;
    private AppProperties appProperties;
    private RecoveryService recoveryService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        emailTokenRepository = mock(EmailTokenRepository.class);
        trustedDeviceRepository = mock(TrustedDeviceRepository.class);
        passwordEncoder = mock(Argon2SecurityUtil.class);
        passwordPolicyService = mock(PasswordPolicyService.class);
        emailService = mock(EmailService.class);
        auditService = mock(AuditService.class);
        reasonCatalogService = new ReasonCatalogService();
        appProperties = mock(AppProperties.class);
        when(appProperties.getEncKey()).thenReturn("0123456789abcdef0123456789abcdef");

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
    }

    @Test
    void testRequestRecovery_VerifiedUser_GeneratesTokenAndSendsEmail() {
        User user = new User("alice@example.com", "Alice", "hash");
        user.setId("u1");
        user.setEmailVerified(true);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        RecoveryRequestDto req = new RecoveryRequestDto("alice@example.com");
        GenericResponse resp = recoveryService.requestRecovery(req, "sess1", "127.0.0.1");

        assertEquals("OK", resp.getStatus());
        verify(emailTokenRepository).save(any(EmailToken.class));
        verify(emailService).sendEmail(eq("sess1"), eq("alice@example.com"), anyString(), anyString());
        verify(auditService).logEvent(eq("u1"), eq("alice@example.com"), eq(LoginEventType.RECOVERY_REQUESTED),
                isNull(), isNull(), isNull(), eq("RECOVERY_SENT"), isNull(), isNull());
    }

    @Test
    void testRequestRecovery_UnknownOrUnverifiedUser_DoesNotSendEmail_ReturnsGenericResponse() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        RecoveryRequestDto req = new RecoveryRequestDto("unknown@example.com");
        GenericResponse resp = recoveryService.requestRecovery(req, "sess1", "127.0.0.1");

        assertEquals("OK", resp.getStatus());
        verify(emailTokenRepository, never()).save(any());
        verify(emailService, never()).sendEmail(any(), any(), any(), any());
        verify(passwordEncoder).verifyAgainstDummyHash(anyString());
    }

    @Test
    void testValidateToken_ValidToken_ReturnsRequiresSecondFactorStatus() {
        String rawToken = "recovery-token-123";
        String tokenHash = CryptoUtil.sha256(rawToken);

        EmailToken token = new EmailToken("u1", TokenType.RECOVERY, tokenHash, Instant.now().plusSeconds(600));
        User user = new User("alice@example.com", "Alice", "hash");
        user.setId("u1");
        user.setEmailVerified(true);
        user.setTotpEnabled(true);

        when(emailTokenRepository.findByTokenHashAndType(tokenHash, TokenType.RECOVERY)).thenReturn(Optional.of(token));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        RecoveryValidateResponse resp = recoveryService.validateToken(rawToken);
        assertTrue(resp.isValid());
        assertTrue(resp.isRequiresSecondFactor());
    }

    @Test
    void testResetPassword_WithTotpEnabled_RequiresSecondFactor() {
        String rawToken = "recovery-token-123";
        String tokenHash = CryptoUtil.sha256(rawToken);

        EmailToken token = new EmailToken("u1", TokenType.RECOVERY, tokenHash, Instant.now().plusSeconds(600));
        User user = new User("alice@example.com", "Alice", "old-hash");
        user.setId("u1");
        user.setEmailVerified(true);
        user.setTotpEnabled(true);

        when(emailTokenRepository.findByTokenHashAndType(tokenHash, TokenType.RECOVERY)).thenReturn(Optional.of(token));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        MockHttpServletRequest request = new MockHttpServletRequest();

        // Omit second factor code
        RecoveryResetRequest resetReq = new RecoveryResetRequest(rawToken, "NewSecurePassword123!", null);

        UserService.CustomAuthException ex = assertThrows(UserService.CustomAuthException.class,
                () -> recoveryService.resetPassword(resetReq, request));
        assertEquals("OTP_INVALID", ex.getReasonCode());
    }

    @Test
    void testResetPassword_ValidBackupCode_CompletesRecoveryAndRevokesDevices() {
        String rawToken = "recovery-token-123";
        String tokenHash = CryptoUtil.sha256(rawToken);

        String rawBackup = "B1C2-D3E4";
        String backupHash = CryptoUtil.sha256(rawBackup.toUpperCase());

        EmailToken token = new EmailToken("u1", TokenType.RECOVERY, tokenHash, Instant.now().plusSeconds(600));
        User user = new User("alice@example.com", "Alice", "old-hash");
        user.setId("u1");
        user.setEmailVerified(true);
        user.setTotpEnabled(true);
        user.setBackupCodeHashes(new ArrayList<>(List.of(backupHash)));

        when(emailTokenRepository.findByTokenHashAndType(tokenHash, TokenType.RECOVERY)).thenReturn(Optional.of(token));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewSecurePassword123!")).thenReturn("new-encoded-hash");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true);

        RecoveryResetRequest resetReq = new RecoveryResetRequest(rawToken, "NewSecurePassword123!", rawBackup);
        GenericResponse resp = recoveryService.resetPassword(resetReq, request);

        assertEquals("OK", resp.getStatus());
        assertNotNull(token.getUsedAt());
        assertEquals("new-encoded-hash", user.getPasswordHash());
        assertFalse(user.getBackupCodeHashes().contains(backupHash)); // consumed

        // Devices revoked & alert sent
        verify(trustedDeviceRepository).deleteByUserId("u1");
        verify(emailService).sendPasswordChangedAlert(anyString(), eq("alice@example.com"));
        verify(auditService).logEvent(eq("u1"), eq("alice@example.com"), eq(LoginEventType.RECOVERY_COMPLETED),
                isNull(), isNull(), isNull(), eq("RECOVERY_COMPLETED"), isNull(), isNull());
    }

    @Test
    void testResetPassword_TokenSingleUse() {
        String rawToken = "used-token";
        String tokenHash = CryptoUtil.sha256(rawToken);

        EmailToken token = new EmailToken("u1", TokenType.RECOVERY, tokenHash, Instant.now().plusSeconds(600));
        token.setUsedAt(Instant.now().minusSeconds(10)); // already used

        when(emailTokenRepository.findByTokenHashAndType(tokenHash, TokenType.RECOVERY)).thenReturn(Optional.of(token));

        MockHttpServletRequest request = new MockHttpServletRequest();
        RecoveryResetRequest resetReq = new RecoveryResetRequest(rawToken, "NewSecurePassword123!", null);

        UserService.CustomAuthException ex = assertThrows(UserService.CustomAuthException.class,
                () -> recoveryService.resetPassword(resetReq, request));
        assertEquals("TOKEN_INVALID_OR_EXPIRED", ex.getReasonCode());
    }
}
