package com.authease.service;

import com.authease.dto.GenericResponse;
import com.authease.dto.RegisterRequest;
import com.authease.model.*;
import com.authease.repository.EmailTokenRepository;
import com.authease.repository.UserRepository;
import com.authease.security.Argon2SecurityUtil;
import com.authease.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private EmailTokenRepository emailTokenRepository;
    private Argon2SecurityUtil passwordEncoder;
    private EmailService emailService;
    private AuditService auditService;
    private ReasonCatalogService reasonCatalogService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        emailTokenRepository = mock(EmailTokenRepository.class);
        passwordEncoder = mock(Argon2SecurityUtil.class);
        emailService = mock(EmailService.class);
        auditService = mock(AuditService.class);
        reasonCatalogService = new ReasonCatalogService();

        userService = new UserService(userRepository, emailTokenRepository, passwordEncoder,
                emailService, auditService, reasonCatalogService);
    }

    @Test
    void testRegister_NewUser_CreatesUnverifiedUserAndSendsVerificationEmail() {
        RegisterRequest req = new RegisterRequest("newuser@example.com", "New User", "Password123!");
        when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed-pwd");

        GenericResponse resp = userService.register(req, "session-1", "127.0.0.1");
        assertEquals("OK", resp.getStatus());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertFalse(userCaptor.getValue().isEmailVerified());
        assertEquals("newuser@example.com", userCaptor.getValue().getEmail());

        verify(emailTokenRepository).save(any(EmailToken.class));
        verify(emailService).sendVerificationEmail(eq("session-1"), eq("newuser@example.com"), anyString());
    }

    @Test
    void testRegister_ExistingUser_SendsAlreadyRegisteredEmail_GenericOkResponse() {
        RegisterRequest req = new RegisterRequest("existing@example.com", "Existing", "Password123!");
        User existingUser = new User("existing@example.com", "Existing", "hashed");
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));

        GenericResponse resp = userService.register(req, "session-1", "127.0.0.1");
        assertEquals("OK", resp.getStatus());

        verify(userRepository, never()).save(any());
        verify(emailService).sendAlreadyRegisteredEmail("session-1", "existing@example.com");
    }

    @Test
    void testVerifyEmail_ValidToken_MarksUserVerified() {
        String rawToken = "raw-token-123";
        String tokenHash = CryptoUtil.sha256(rawToken);

        User user = new User("user@example.com", "User", "hashed");
        user.setId("u123");
        user.setEmailVerified(false);

        EmailToken token = new EmailToken("u123", TokenType.VERIFY, tokenHash, Instant.now().plusSeconds(3600));

        when(emailTokenRepository.findByTokenHashAndType(tokenHash, TokenType.VERIFY)).thenReturn(Optional.of(token));
        when(userRepository.findById("u123")).thenReturn(Optional.of(user));

        GenericResponse resp = userService.verifyEmail(rawToken, "127.0.0.1");
        assertEquals("OK", resp.getStatus());
        assertTrue(user.isEmailVerified());
        assertNotNull(token.getUsedAt());

        verify(userRepository).save(user);
        verify(emailTokenRepository).save(token);
    }

    @Test
    void testVerifyEmail_ExpiredToken_ThrowsException() {
        String rawToken = "expired-token";
        String tokenHash = CryptoUtil.sha256(rawToken);

        EmailToken token = new EmailToken("u123", TokenType.VERIFY, tokenHash, Instant.now().minusSeconds(10));
        when(emailTokenRepository.findByTokenHashAndType(tokenHash, TokenType.VERIFY)).thenReturn(Optional.of(token));

        UserService.CustomAuthException ex = assertThrows(UserService.CustomAuthException.class,
                () -> userService.verifyEmail(rawToken, "127.0.0.1"));
        assertEquals("TOKEN_INVALID_OR_EXPIRED", ex.getReasonCode());
    }
}
