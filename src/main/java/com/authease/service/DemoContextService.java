package com.authease.service;

import com.authease.config.AppProperties;
import com.authease.dto.DemoContextDto;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DemoContextService {

    private final AppProperties appProperties;
    private final Map<String, DemoContextDto> sessionOverrides = new ConcurrentHashMap<>();

    public DemoContextService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public boolean isDemoMode() {
        return appProperties.isDemoMode();
    }

    public DemoContextDto getContext(String demoSessionId) {
        ensureDemoMode();
        return sessionOverrides.getOrDefault(demoSessionId, new DemoContextDto());
    }

    public DemoContextDto setContext(String demoSessionId, DemoContextDto dto) {
        ensureDemoMode();
        if (dto == null) {
            sessionOverrides.remove(demoSessionId);
            return new DemoContextDto();
        }
        DemoContextDto existing = sessionOverrides.computeIfAbsent(demoSessionId, k -> new DemoContextDto());
        if (dto.getNewDevice() != null) existing.setNewDevice(dto.getNewDevice());
        if (dto.getNewNetwork() != null) existing.setNewNetwork(dto.getNewNetwork());
        if (dto.getUnusualHour() != null) existing.setUnusualHour(dto.getUnusualHour());
        if (dto.getHourOfDay() != null) existing.setHourOfDay(dto.getHourOfDay());
        if (dto.getRecentFailures() != null) existing.setRecentFailures(dto.getRecentFailures());
        if (dto.getForceFailures() != null) existing.setForceFailures(dto.getForceFailures());
        if (dto.getHighRiskFlag() != null) existing.setHighRiskFlag(dto.getHighRiskFlag());
        sessionOverrides.put(demoSessionId, existing);
        return existing;
    }

    public DemoContextDto consumeNextContext(String demoSessionId) {
        if (!isDemoMode() || demoSessionId == null) {
            return null;
        }
        return sessionOverrides.remove(demoSessionId);
    }

    public void clear(String demoSessionId) {
        if (demoSessionId != null) {
            sessionOverrides.remove(demoSessionId);
        }
    }

    private void ensureDemoMode() {
        if (!appProperties.isDemoMode()) {
            throw new UserService.CustomAuthException(
                    "FORBIDDEN",
                    "Demo mode is not active",
                    "Demo simulation controls are disabled on this environment.",
                    "Enable demo mode in application configuration to use simulation controls."
            );
        }
    }
}
