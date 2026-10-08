package com.authease.policy;

import com.authease.model.RiskLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PolicyDecision {

    private final RiskLevel level;
    private final int requiredSteps;
    private final List<String> methods;
    private final boolean needsEmailApproval;
    private final String reasonCode;
    private final boolean suspiciousDetected;

    public PolicyDecision(RiskLevel level, int requiredSteps, List<String> methods,
                          boolean needsEmailApproval, String reasonCode, boolean suspiciousDetected) {
        this.level = level;
        this.requiredSteps = requiredSteps;
        this.methods = methods != null ? Collections.unmodifiableList(methods) : Collections.emptyList();
        this.needsEmailApproval = needsEmailApproval;
        this.reasonCode = reasonCode;
        this.suspiciousDetected = suspiciousDetected;
    }

    public RiskLevel getLevel() {
        return level;
    }

    public int getRequiredSteps() {
        return requiredSteps;
    }

    public List<String> getMethods() {
        return methods;
    }

    public boolean isNeedsEmailApproval() {
        return needsEmailApproval;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public boolean isSuspiciousDetected() {
        return suspiciousDetected;
    }
}
