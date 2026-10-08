package com.authease.policy;

import com.authease.model.RiskLevel;
import com.authease.risk.RiskResult;
import com.authease.risk.RiskSignal;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PolicyService {

    public PolicyDecision applyPolicy(PolicyContext context) {
        RiskResult riskResult = context.getRiskResult();
        RiskLevel level = riskResult != null ? riskResult.getLevel() : RiskLevel.LOW;
        String reasonCode = determineDominantSignal(riskResult);
        boolean suspiciousDetected = false;

        // Hard Rule 1: 5 or more failures for this ip+identifier -> at least HIGH, even for trusted devices.
        if (context.getRecentFailuresIpIdentifier() >= 5) {
            level = RiskLevel.HIGH;
            reasonCode = "MANY_FAILURES";
        }

        // Hard Rule 2: Untrusted device and user has MFA enabled -> at least MEDIUM.
        if (context.isUntrustedDevice() && context.isMfaEnabled()) {
            if (level == RiskLevel.LOW) {
                level = RiskLevel.MEDIUM;
                if (reasonCode == null) reasonCode = "NEW_DEVICE";
            }
        }

        // Hard Rule 3: Account-wide failures >= 10 from many IPs -> require at least MEDIUM for untrusted devices ONLY
        // (trusted devices are unaffected, so an attacker cannot lock out the real user)
        if (context.getAccountWideFailuresManyIps() >= 10) {
            suspiciousDetected = true;
            if (context.isUntrustedDevice()) {
                if (level == RiskLevel.LOW) {
                    level = RiskLevel.MEDIUM;
                    reasonCode = "MANY_FAILURES";
                }
            }
        }

        // Hard Rule 4: ADMIN accounts are always at least MEDIUM
        if (context.isAdmin()) {
            if (level == RiskLevel.LOW) {
                level = RiskLevel.MEDIUM;
                if (reasonCode == null) reasonCode = "NEW_DEVICE";
            }
        }

        // Hard Rule 5: Sensitive actions always require re-verification regardless of score
        if (context.isSensitiveAction()) {
            if (level == RiskLevel.LOW) {
                level = RiskLevel.MEDIUM;
            }
        }

        // Steps mapping: LOW=1, MEDIUM=2, HIGH=3
        int requiredSteps;
        boolean needsEmailApproval;
        List<String> methods = new ArrayList<>();

        if (level == RiskLevel.LOW) {
            requiredSteps = 1;
            needsEmailApproval = false;
        } else if (level == RiskLevel.MEDIUM) {
            requiredSteps = 2;
            needsEmailApproval = false;
            if (context.isMfaEnabled()) {
                methods.add("TOTP");
            } else {
                methods.add("EMAIL_OTP");
            }
            methods.add("BACKUP_CODE");
        } else {
            // HIGH
            requiredSteps = 3;
            needsEmailApproval = true;
            if (context.isMfaEnabled()) {
                methods.add("TOTP");
            } else {
                methods.add("EMAIL_OTP");
            }
            methods.add("BACKUP_CODE");
        }

        if (reasonCode == null) {
            reasonCode = (level == RiskLevel.LOW) ? "INVALID_CREDENTIALS" : "NEW_DEVICE";
        }

        return new PolicyDecision(level, requiredSteps, methods, needsEmailApproval, reasonCode, suspiciousDetected);
    }

    private String determineDominantSignal(RiskResult riskResult) {
        if (riskResult == null || riskResult.getSignals().isEmpty()) {
            return null;
        }
        // Select highest point positive signal
        return riskResult.getSignals().stream()
                .filter(s -> s.getPoints() > 0)
                .max((s1, s2) -> Integer.compare(s1.getPoints(), s2.getPoints()))
                .map(RiskSignal::getCode)
                .orElse(null);
    }
}
