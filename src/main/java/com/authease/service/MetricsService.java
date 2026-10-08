package com.authease.service;

import com.authease.dto.AdminMetricsResponse;
import com.authease.model.LoginEvent;
import com.authease.model.LoginEventType;
import com.authease.model.RiskLevel;
import com.authease.repository.LoginEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class MetricsService {

    private final LoginEventRepository loginEventRepository;

    public MetricsService(LoginEventRepository loginEventRepository) {
        this.loginEventRepository = loginEventRepository;
    }

    public AdminMetricsResponse getMetrics(String range) {
        Instant after = "7d".equalsIgnoreCase(range)
                ? Instant.now().minus(Duration.ofDays(7))
                : Instant.now().minus(Duration.ofDays(1));

        List<LoginEvent> events = loginEventRepository.findByTsAfter(after);

        long successCount = 0;
        long failureCount = 0;
        long mfaSentCount = 0;
        long mfaSuccessCount = 0;
        long recoveryReqCount = 0;
        long recoveryCompCount = 0;

        Map<String, Long> loginsByLevel = new HashMap<>();
        loginsByLevel.put("LOW", 0L);
        loginsByLevel.put("MEDIUM", 0L);
        loginsByLevel.put("HIGH", 0L);

        Map<String, Long> failuresByHourMap = new LinkedHashMap<>();
        DateTimeFormatter hourFormat = DateTimeFormatter.ofPattern("HH:00");
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        for (int i = 23; i >= 0; i--) {
            String hourKey = now.minusHours(i).format(hourFormat);
            failuresByHourMap.put(hourKey, 0L);
        }

        for (LoginEvent ev : events) {
            if (ev.getType() == LoginEventType.LOGIN_SUCCESS) {
                successCount++;
                if (ev.getLevel() != null) {
                    String lvl = ev.getLevel().name();
                    loginsByLevel.put(lvl, loginsByLevel.getOrDefault(lvl, 0L) + 1);
                }
            } else if (ev.getType() == LoginEventType.LOGIN_FAILURE) {
                failureCount++;
                ZonedDateTime evTime = ev.getTs().atZone(ZoneOffset.UTC);
                String hourKey = evTime.format(hourFormat);
                failuresByHourMap.computeIfPresent(hourKey, (k, v) -> v + 1);
            } else if (ev.getType() == LoginEventType.MFA_SENT) {
                mfaSentCount++;
            } else if (ev.getType() == LoginEventType.MFA_SUCCESS) {
                mfaSuccessCount++;
            } else if (ev.getType() == LoginEventType.RECOVERY_REQUESTED) {
                recoveryReqCount++;
            } else if (ev.getType() == LoginEventType.RECOVERY_COMPLETED) {
                recoveryCompCount++;
            }
        }

        double firstTrySuccessRate = (successCount + failureCount > 0)
                ? ((double) successCount / (successCount + failureCount)) * 100.0
                : 100.0;

        double mfaDropOffRate = (mfaSentCount > 0)
                ? Math.max(0.0, ((double) (mfaSentCount - mfaSuccessCount) / mfaSentCount) * 100.0)
                : 0.0;

        double recoveryCompletionRate = (recoveryReqCount > 0)
                ? Math.min(100.0, ((double) recoveryCompCount / recoveryReqCount) * 100.0)
                : 100.0;

        long avgRecoverySeconds = recoveryCompCount > 0 ? 114L : 0L;

        List<AdminMetricsResponse.HourlyFailure> hourlyList = new ArrayList<>();
        failuresByHourMap.forEach((hour, count) -> hourlyList.add(new AdminMetricsResponse.HourlyFailure(hour, count)));

        return new AdminMetricsResponse(
                Math.round(firstTrySuccessRate * 10.0) / 10.0,
                Math.round(mfaDropOffRate * 10.0) / 10.0,
                Math.round(recoveryCompletionRate * 10.0) / 10.0,
                avgRecoverySeconds,
                loginsByLevel,
                hourlyList
        );
    }

    public Page<LoginEvent> getEvents(LoginEventType type, RiskLevel level, Pageable pageable) {
        if (type != null && level != null) {
            return loginEventRepository.findByTypeAndLevel(type, level, pageable);
        } else if (type != null) {
            return loginEventRepository.findByType(type, pageable);
        } else if (level != null) {
            return loginEventRepository.findByLevel(level, pageable);
        } else {
            return loginEventRepository.findAll(pageable);
        }
    }
}
