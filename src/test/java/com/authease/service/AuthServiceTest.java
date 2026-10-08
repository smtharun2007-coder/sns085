package com.authease.service;

import com.authease.dto.AuthMeResponse;
import com.authease.dto.GenericResponse;
import com.authease.dto.LoginRequest;
import com.authease.dto.LoginResponse;
import com.authease.model.Challenge;
import com.authease.model.RiskLevel;
import com.authease.model.User;
import com.authease.policy.PolicyDecision;
import com.authease.policy.PolicyService;
import com.authease.repository.LoginEventRepository;
import com.authease.repository.TrustedDeviceRepository;
import com.authease.repository.UserRepository;
import com.authease.risk.RiskEngine;
import com.authease.risk.RiskResult;
import com.authease.security.Argon2SecurityUtil;
import com.authease.security.ClientIpResolver;
import com.authease.security.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private TrustedDeviceRepository trustedDeviceRepository;
    private LoginEventRepository loginEventRepository;
    private Argon2SecurityUtil passwordEncoder;
    private RateLimitService rateLimitService;
    private ClientIpResolver clientIpResolver;
    private AuditService auditService;
    private EmailService emailService;
    private ReasonCatalogService reasonCatalogService;
    private RiskEngine riskEngine;
    private PolicyService policyService;
    private ChallengeService challengeService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        trustedDeviceRepository = mock(TrustedDeviceRepository.class);
        loginEventRepository = mock(LoginEventRepository.class);
        passwordEncoder = mock(Argon2SecurityUtil.class);
        rateLimitService = mock(RateLimitService.class);
        clientIpResolver = mock(ClientIpResolver.class);
        auditService = mock(AuditService.class);
        emailService = mock(EmailService.class);
        reasonCatalogService = new ReasonCatalogService();
        riskEngine = mock(RiskEngine.class);
        policyService = mock(PolicyService.class);
        challengeService = mock(ChallengeService.class);

        authService = new AuthService(
                userRepository,
                trustedDeviceRepository,
                loginEventRepository,
                passwordEncoder,
                rateLimitService,
                clientIpResolver,
                auditService,
                emailService,
                reasonCatalogService,
                riskEngine,
                policyService,
                challengeService
        );
    }

    @Test
    void testLogin_RateDelayed_ReturnsDelayedResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(clientIpResolver.resolveClientIp(request)).thenReturn("127.0.0.1");
        when(rateLimitService.checkRateLimit(eq("127.0.0.1"), eq("user@example.com")))
                .thenReturn(new RateLimitService.RateLimitStatus(true, 15, 3));

        LoginRequest loginRequest = new LoginRequest("user@example.com", "password123");
        LoginResponse response = authService.login(loginRequest, request);

        assertEquals("DELAYED", response.getStatus());
        assertEquals(15L, response.getRetryAfterSeconds());
        assertEquals("RATE_DELAY", response.getReasonCode());
    }

    @Test
    void testLogin_InvalidPassword_ReturnsFailedWithTimingDefense() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(clientIpResolver.resolveClientIp(request)).thenReturn("127.0.0.1");
        when(rateLimitService.checkRateLimit(anyString(), anyString()))
                .thenReturn(new RateLimitService.RateLimitStatus(false, 0, 0));

        User user = new User("user@example.com", "Test User", "encoded-hash");
        user.setEmailVerified(true);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-hash")).thenReturn(false);

        LoginRequest loginRequest = new LoginRequest("user@example.com", "wrong-password");
        LoginResponse response = authService.login(loginRequest, request);

        assertEquals("FAILED", response.getStatus());
        assertEquals("INVALID_CREDENTIALS", response.getReasonCode());
        verify(rateLimitService).recordFailure("127.0.0.1", "user@example.com");
    }

    @Test
    void testLogin_UnverifiedEmail_ReturnsEmailNotVerified() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(clientIpResolver.resolveClientIp(request)).thenReturn("127.0.0.1");
        when(rateLimitService.checkRateLimit(anyString(), anyString()))
                .thenReturn(new RateLimitService.RateLimitStatus(false, 0, 0));

        User user = new User("unverified@example.com", "Unverified User", "encoded-hash");
        user.setEmailVerified(false);
        when(userRepository.findByEmail("unverified@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("valid-password", "encoded-hash")).thenReturn(true);

        LoginRequest loginRequest = new LoginRequest("unverified@example.com", "valid-password");
        LoginResponse response = authService.login(loginRequest, request);

        assertEquals("FAILED", response.getStatus());
        assertEquals("EMAIL_NOT_VERIFIED", response.getReasonCode());
    }

    @Test
    void testLogin_LowRiskPolicy_AuthenticatesDirectly() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(clientIpResolver.resolveClientIp(request)).thenReturn("127.0.0.1");
        when(clientIpResolver.getNetworkPrefixHash(anyString())).thenReturn("net-hash-123");
        when(rateLimitService.checkRateLimit(anyString(), anyString()))
                .thenReturn(new RateLimitService.RateLimitStatus(false, 0, 0));

        User user = new User("verified@example.com", "Verified User", "encoded-hash");
        user.setId("u100");
        user.setEmailVerified(true);
        user.setRoles(Set.of("USER"));
        when(userRepository.findByEmail("verified@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("valid-password", "encoded-hash")).thenReturn(true);

        when(riskEngine.evaluate(any())).thenReturn(new RiskResult(10, RiskLevel.LOW, Collections.emptyList()));
        when(policyService.applyPolicy(any())).thenReturn(
                new PolicyDecision(RiskLevel.LOW, 1, Collections.emptyList(), false, "LOGIN_SUCCESS", false));

        LoginRequest loginRequest = new LoginRequest("verified@example.com", "valid-password");
        LoginResponse response = authService.login(loginRequest, request);

        assertEquals("AUTHENTICATED", response.getStatus());
        verify(rateLimitService).clear("127.0.0.1", "verified@example.com");
        assertEquals("u100", request.getSession().getAttribute(AuthService.SESSION_USER_ID));
    }

    @Test
    void testLogin_MediumRiskPolicy_ReturnsMfaRequired() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(clientIpResolver.resolveClientIp(request)).thenReturn("127.0.0.1");
        when(clientIpResolver.getNetworkPrefixHash(anyString())).thenReturn("net-hash-123");
        when(rateLimitService.checkRateLimit(anyString(), anyString()))
                .thenReturn(new RateLimitService.RateLimitStatus(false, 0, 0));

        User user = new User("user@example.com", "User", "encoded-hash");
        user.setId("u100");
        user.setEmailVerified(true);
        user.setTotpEnabled(true);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("valid-password", "encoded-hash")).thenReturn(true);

        when(riskEngine.evaluate(any())).thenReturn(new RiskResult(35, RiskLevel.MEDIUM, Collections.emptyList()));
        when(policyService.applyPolicy(any())).thenReturn(
                new PolicyDecision(RiskLevel.MEDIUM, 2, List.of("TOTP", "BACKUP_CODE"), false, "NEW_DEVICE", false));

        Challenge challenge = new Challenge("c100", "u100", "sess-1", RiskLevel.MEDIUM, 2, null, Instant.now().plusSeconds(600));
        when(challengeService.createChallenge(eq("u100"), anyString(), eq(RiskLevel.MEDIUM), eq(2)))
                .thenReturn(new ChallengeService.ChallengeCreation(challenge, null));

        LoginRequest loginRequest = new LoginRequest("user@example.com", "valid-password");
        LoginResponse response = authService.login(loginRequest, request);

        assertEquals("MFA_REQUIRED", response.getStatus());
        assertEquals("c100", response.getChallengeId());
        assertEquals("MEDIUM", response.getLevel());
        assertFalse(response.getNeedsEmailApproval());
        assertEquals(1, response.getStep().getCurrent());
        assertEquals(2, response.getStep().getTotal());
        assertEquals("NEW_DEVICE", response.getReasonCode());
    }

    @Test
    void testLogin_HighRiskPolicy_DispatchesApprovalAndAlertEmail() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(clientIpResolver.resolveClientIp(request)).thenReturn("127.0.0.1");
        when(clientIpResolver.getNetworkPrefixHash(anyString())).thenReturn("net-hash-123");
        when(rateLimitService.checkRateLimit(anyString(), anyString()))
                .thenReturn(new RateLimitService.RateLimitStatus(false, 0, 0));

        User user = new User("user@example.com", "User", "encoded-hash");
        user.setId("u100");
        user.setEmailVerified(true);
        user.setTotpEnabled(false);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("valid-password", "encoded-hash")).thenReturn(true);

        when(riskEngine.evaluate(any())).thenReturn(new RiskResult(80, RiskLevel.HIGH, Collections.emptyList()));
        when(policyService.applyPolicy(any())).thenReturn(
                new PolicyDecision(RiskLevel.HIGH, 3, List.of("EMAIL_OTP", "BACKUP_CODE"), true, "MANY_FAILURES", false));

        Challenge challenge = new Challenge("c200", "u100", "sess-1", RiskLevel.HIGH, 3,
                Instant.now().plusSeconds(30), Instant.now().plusSeconds(600));
        when(challengeService.createChallenge(eq("u100"), anyString(), eq(RiskLevel.HIGH), eq(3)))
                .thenReturn(new ChallengeService.ChallengeCreation(challenge, "raw-approval-token"));

        LoginRequest loginRequest = new LoginRequest("user@example.com", "valid-password");
        LoginResponse response = authService.login(loginRequest, request);

        assertEquals("MFA_REQUIRED", response.getStatus());
        assertEquals("c200", response.getChallengeId());
        assertEquals("HIGH", response.getLevel());
        assertTrue(response.getNeedsEmailApproval());
        assertEquals(1, response.getStep().getCurrent());
        assertEquals(3, response.getStep().getTotal());
        verify(emailService).sendEmailApproval(anyString(), eq("user@example.com"), eq("raw-approval-token"));
    }

    @Test
    void testMe_AuthenticatedUser() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute(AuthService.SESSION_USER_ID, "u100");

        User user = new User("verified@example.com", "Verified User", "encoded-hash");
        user.setId("u100");
        user.setRoles(Set.of("USER"));
        user.setTotpEnabled(false);
        when(userRepository.findById("u100")).thenReturn(Optional.of(user));

        AuthMeResponse me = authService.me(request);
        assertTrue(me.isAuthenticated());
        assertNotNull(me.getUser());
        assertEquals("verified@example.com", me.getUser().getEmail());
    }

    @Test
    void testLogout_ClearsSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute(AuthService.SESSION_USER_ID, "u100");

        GenericResponse resp = authService.logout(request);
        assertEquals("OK", resp.getStatus());
        assertNull(request.getSession(false));
    }
}
