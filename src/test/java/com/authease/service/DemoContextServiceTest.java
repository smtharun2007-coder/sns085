package com.authease.service;

import com.authease.config.AppProperties;
import com.authease.dto.DemoContextDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DemoContextServiceTest {

    private AppProperties appProperties;
    private DemoContextService demoContextService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.setDemoMode(true);
        demoContextService = new DemoContextService(appProperties);
    }

    @Test
    void testGetAndSetContext() {
        DemoContextDto dto = new DemoContextDto();
        dto.setNewDevice(true);
        dto.setRecentFailures(3);

        demoContextService.setContext("sess-1", dto);

        DemoContextDto fetched = demoContextService.getContext("sess-1");
        assertTrue(fetched.getNewDevice());
        assertEquals(3, fetched.getRecentFailures());
    }

    @Test
    void testConsumeNextContextRemovesOverride() {
        DemoContextDto dto = new DemoContextDto();
        dto.setUnusualHour(true);
        demoContextService.setContext("sess-2", dto);

        DemoContextDto consumed = demoContextService.consumeNextContext("sess-2");
        assertNotNull(consumed);
        assertTrue(consumed.getUnusualHour());

        DemoContextDto after = demoContextService.consumeNextContext("sess-2");
        assertNull(after);
    }

    @Test
    void testThrowsWhenDemoModeDisabled() {
        appProperties.setDemoMode(false);
        assertThrows(UserService.CustomAuthException.class, () -> demoContextService.getContext("sess-3"));
    }
}
