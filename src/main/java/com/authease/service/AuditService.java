package com.authease.service;

import com.authease.model.LoginEvent;
import com.authease.model.LoginEventType;
import com.authease.model.RiskLevel;
import com.authease.repository.LoginEventRepository;
import com.authease.util.CryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final LoginEventRepository loginEventRepository;

    public AuditService(LoginEventRepository loginEventRepository) {
        this.loginEventRepository = loginEventRepository;
    }

    public void logEvent(String userIdOrNull, String identifier, LoginEventType type,
                         RiskLevel level, Integer score, List<String> signals,
                         String reasonCode, String ipPrefixHash, String deviceId) {
        try {
            String identifierHash = identifier != null ? CryptoUtil.sha256(identifier.toLowerCase().trim()) : null;
            LoginEvent event = new LoginEvent(
                    Instant.now(),
                    userIdOrNull,
                    identifierHash,
                    type,
                    level,
                    score,
                    signals,
                    reasonCode,
                    ipPrefixHash,
                    deviceId
            );
            loginEventRepository.save(event);
            log.info("Audit event logged: type={}, user={}, level={}, reason={}",
                    type, userIdOrNull != null ? "user-" + userIdOrNull.substring(0, Math.min(6, userIdOrNull.length())) : "null",
                    level, reasonCode);
        } catch (Exception e) {
            log.error("Failed to persist audit event", e);
        }
    }
}
