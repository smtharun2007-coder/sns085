package com.authease.service;

import com.authease.dto.AccountSecurityResponse;
import com.authease.dto.LastDecisionResponse;
import com.authease.model.LoginEvent;
import com.authease.model.RiskLevel;
import com.authease.model.TrustedDevice;
import com.authease.model.User;
import com.authease.repository.LoginEventRepository;
import com.authease.repository.TrustedDeviceRepository;
import com.authease.repository.UserRepository;
import com.authease.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountServiceTest {

    private UserRepository userRepository;
    private TrustedDeviceRepository trustedDeviceRepository;
    private LoginEventRepository loginEventRepository;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        trustedDeviceRepository = Mockito.mock(TrustedDeviceRepository.class);
        loginEventRepository = Mockito.mock(LoginEventRepository.class);
        accountService = new AccountService(userRepository, trustedDeviceRepository, loginEventRepository);
    }

    @Test
    void testGetSecurityInfo() {
        User user = new User();
        user.setId("u123");
        user.setTotpEnabled(true);

        TrustedDevice dev = new TrustedDevice("u123", CryptoUtil.sha256("cookie-val"), "Chrome on Windows", Instant.now().plusSeconds(3600));
        dev.setId("dev1");

        when(userRepository.findById("u123")).thenReturn(Optional.of(user));
        when(trustedDeviceRepository.findByUserId("u123")).thenReturn(List.of(dev));
        when(loginEventRepository.findByUserIdOrderByTsDesc("u123")).thenReturn(List.of());

        AccountSecurityResponse res = accountService.getSecurityInfo("u123", "cookie-val");
        assertTrue(res.isMfaEnabled());
        assertEquals(1, res.getDevices().size());
        assertTrue(res.getDevices().get(0).isCurrent());
    }

    @Test
    void testDeleteDevice() {
        TrustedDevice dev = new TrustedDevice("u123", "iPhone", "hash", Instant.now().plusSeconds(3600));
        dev.setId("dev1");

        when(trustedDeviceRepository.findById("dev1")).thenReturn(Optional.of(dev));

        accountService.deleteDevice("u123", "dev1");
        verify(trustedDeviceRepository, times(1)).delete(dev);
    }

    @Test
    void testGetLastDecision() {
        LoginEvent event = new LoginEvent();
        event.setLevel(RiskLevel.MEDIUM);
        event.setScore(45);
        event.setSignals(List.of("NEW_DEVICE", "UNUSUAL_HOUR"));

        when(loginEventRepository.findFirstByUserIdOrderByTsDesc("u123")).thenReturn(Optional.of(event));

        LastDecisionResponse res = accountService.getLastDecision("u123");
        assertEquals(45, res.getScore());
        assertEquals("MEDIUM", res.getLevel());
        assertEquals(2, res.getSignals().size());
    }
}
