package com.authease.risk;

import com.authease.model.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RiskEngine {

    public static final int BASE_SCORE = 10;

    public RiskResult evaluate(RiskContext context) {
        int score = BASE_SCORE;
        List<RiskSignal> signals = new ArrayList<>();

        // BASELINE RULE: "new device" and "new network" apply only if the user has at least one previous successful login
        boolean previousLogin = context.hasPreviousSuccessfulLogin();

        // Positive Signals
        if (previousLogin && context.isNewDevice()) {
            score += 25;
            signals.add(new RiskSignal("NEW_DEVICE", "Sign-in from a new device", 25));
        }

        if (previousLogin && context.isNewNetwork()) {
            score += 20;
            signals.add(new RiskSignal("NEW_NETWORK", "Sign-in from a new network", 20));
        }

        int failures = context.getRecentFailures();
        if (failures >= 3) {
            score += 25;
            signals.add(new RiskSignal("MANY_FAILURES", "3 or more recent incorrect attempts", 25));
        } else if (failures >= 1) {
            score += 10;
            signals.add(new RiskSignal("RECENT_FAILURES", "Recent incorrect attempts", 10));
        }

        if (context.isUnusualHour()) {
            score += 10;
            signals.add(new RiskSignal("UNUSUAL_HOUR", "Unusual sign-in hour", 10));
        }

        // Trust Signals
        if (context.isTrustedDevice()) {
            score -= 20;
            signals.add(new RiskSignal("TRUSTED_DEVICE", "Recognized trusted device", -20));
        }

        if (context.isRecentSuccessFromSameDevice24h()) {
            score -= 10;
            signals.add(new RiskSignal("RECENT_SUCCESS", "Recent successful sign-in from this device within 24h", -10));
        }

        // Clamp score between 0 and 100
        int finalScore = Math.max(0, Math.min(100, score));

        // Determine Level: 0-30 LOW, 31-60 MEDIUM, 61-100 HIGH
        RiskLevel level;
        if (finalScore <= 30) {
            level = RiskLevel.LOW;
        } else if (finalScore <= 60) {
            level = RiskLevel.MEDIUM;
        } else {
            level = RiskLevel.HIGH;
        }

        return new RiskResult(finalScore, level, signals);
    }
}
