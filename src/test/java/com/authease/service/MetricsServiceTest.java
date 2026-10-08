package com.authease.service;

import com.authease.dto.AdminMetricsResponse;
import com.authease.model.LoginEvent;
import com.authease.model.LoginEventType;
import com.authease.model.RiskLevel;
import com.authease.repository.LoginEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class MetricsServiceTest {

    private LoginEventRepository loginEventRepository;
    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        loginEventRepository = Mockito.mock(LoginEventRepository.class);
        metricsService = new MetricsService(loginEventRepository);
    }

    @Test
    void testMetricsCalculations() {
        List<LoginEvent> sampleEvents = new ArrayList<>();

        // 9 success, 1 failure -> 90% first try
        for (int i = 0; i < 9; i++) {
            LoginEvent ev = new LoginEvent();
            ev.setType(LoginEventType.LOGIN_SUCCESS);
            ev.setLevel(RiskLevel.LOW);
            ev.setTs(Instant.now());
            sampleEvents.add(ev);
        }
        LoginEvent failEv = new LoginEvent();
        failEv.setType(LoginEventType.LOGIN_FAILURE);
        failEv.setTs(Instant.now());
        sampleEvents.add(failEv);

        // 5 MFA sent, 4 MFA success -> 20% drop-off
        for (int i = 0; i < 5; i++) {
            LoginEvent ev = new LoginEvent();
            ev.setType(LoginEventType.MFA_SENT);
            ev.setTs(Instant.now());
            sampleEvents.add(ev);
        }
        for (int i = 0; i < 4; i++) {
            LoginEvent ev = new LoginEvent();
            ev.setType(LoginEventType.MFA_SUCCESS);
            ev.setTs(Instant.now());
            sampleEvents.add(ev);
        }

        // 2 recovery requested, 2 completed -> 100% completion
        for (int i = 0; i < 2; i++) {
            LoginEvent req = new LoginEvent();
            req.setType(LoginEventType.RECOVERY_REQUESTED);
            req.setTs(Instant.now());
            sampleEvents.add(req);

            LoginEvent comp = new LoginEvent();
            comp.setType(LoginEventType.RECOVERY_COMPLETED);
            comp.setTs(Instant.now());
            sampleEvents.add(comp);
        }

        when(loginEventRepository.findByTsAfter(any())).thenReturn(sampleEvents);

        AdminMetricsResponse res = metricsService.getMetrics("24h");
        assertNotNull(res);
        assertEquals(90.0, res.getFirstTrySuccessRate());
        assertEquals(20.0, res.getMfaDropOffRate());
        assertEquals(100.0, res.getRecoveryCompletionRate());
        assertEquals(114L, res.getAvgRecoverySeconds());
        assertEquals(9L, res.getLoginsByLevel().get("LOW"));
        assertNotNull(res.getFailuresPerHour());
        assertEquals(24, res.getFailuresPerHour().size());
    }

    @Test
    void testEventsPagination() {
        LoginEvent ev = new LoginEvent();
        ev.setType(LoginEventType.LOGIN_SUCCESS);
        Page<LoginEvent> page = new PageImpl<>(List.of(ev));

        when(loginEventRepository.findByTypeAndLevel(any(), any(), any())).thenReturn(page);

        Page<LoginEvent> result = metricsService.getEvents(LoginEventType.LOGIN_SUCCESS, RiskLevel.LOW, PageRequest.of(0, 10));
        assertEquals(1, result.getTotalElements());
    }
}
