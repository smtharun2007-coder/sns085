package com.authease.policy;

import com.authease.risk.RiskResult;

public class PolicyContext {

    private final RiskResult riskResult;
    private final int recentFailuresIpIdentifier;
    private final boolean untrustedDevice;
    private final boolean mfaEnabled;
    private final boolean admin;
    private final long accountWideFailuresManyIps;
    private final boolean sensitiveAction;

    public PolicyContext(RiskResult riskResult,
                         int recentFailuresIpIdentifier,
                         boolean untrustedDevice,
                         boolean mfaEnabled,
                         boolean admin,
                         long accountWideFailuresManyIps,
                         boolean sensitiveAction) {
        this.riskResult = riskResult;
        this.recentFailuresIpIdentifier = recentFailuresIpIdentifier;
        this.untrustedDevice = untrustedDevice;
        this.mfaEnabled = mfaEnabled;
        this.admin = admin;
        this.accountWideFailuresManyIps = accountWideFailuresManyIps;
        this.sensitiveAction = sensitiveAction;
    }

    public RiskResult getRiskResult() {
        return riskResult;
    }

    public int getRecentFailuresIpIdentifier() {
        return recentFailuresIpIdentifier;
    }

    public boolean isUntrustedDevice() {
        return untrustedDevice;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public boolean isAdmin() {
        return admin;
    }

    public long getAccountWideFailuresManyIps() {
        return accountWideFailuresManyIps;
    }

    public boolean isSensitiveAction() {
        return sensitiveAction;
    }
}
