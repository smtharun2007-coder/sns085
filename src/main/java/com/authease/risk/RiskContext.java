package com.authease.risk;

public class RiskContext {

    private final boolean hasPreviousSuccessfulLogin;
    private final boolean trustedDevice;
    private final boolean recentSuccessFromSameDevice24h;
    private final boolean newDevice;
    private final boolean newNetwork;
    private final int recentFailures;
    private final boolean unusualHour;

    public RiskContext(boolean hasPreviousSuccessfulLogin,
                       boolean trustedDevice,
                       boolean recentSuccessFromSameDevice24h,
                       boolean newDevice,
                       boolean newNetwork,
                       int recentFailures,
                       boolean unusualHour) {
        this.hasPreviousSuccessfulLogin = hasPreviousSuccessfulLogin;
        this.trustedDevice = trustedDevice;
        this.recentSuccessFromSameDevice24h = recentSuccessFromSameDevice24h;
        this.newDevice = newDevice;
        this.newNetwork = newNetwork;
        this.recentFailures = recentFailures;
        this.unusualHour = unusualHour;
    }

    public boolean hasPreviousSuccessfulLogin() {
        return hasPreviousSuccessfulLogin;
    }

    public boolean isTrustedDevice() {
        return trustedDevice;
    }

    public boolean isRecentSuccessFromSameDevice24h() {
        return recentSuccessFromSameDevice24h;
    }

    public boolean isNewDevice() {
        return newDevice;
    }

    public boolean isNewNetwork() {
        return newNetwork;
    }

    public int getRecentFailures() {
        return recentFailures;
    }

    public boolean isUnusualHour() {
        return unusualHour;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean hasPreviousSuccessfulLogin = false;
        private boolean trustedDevice = false;
        private boolean recentSuccessFromSameDevice24h = false;
        private boolean newDevice = false;
        private boolean newNetwork = false;
        private int recentFailures = 0;
        private boolean unusualHour = false;

        public Builder hasPreviousSuccessfulLogin(boolean val) {
            this.hasPreviousSuccessfulLogin = val;
            return this;
        }

        public Builder trustedDevice(boolean val) {
            this.trustedDevice = val;
            return this;
        }

        public Builder recentSuccessFromSameDevice24h(boolean val) {
            this.recentSuccessFromSameDevice24h = val;
            return this;
        }

        public Builder newDevice(boolean val) {
            this.newDevice = val;
            return this;
        }

        public Builder newNetwork(boolean val) {
            this.newNetwork = val;
            return this;
        }

        public Builder recentFailures(int val) {
            this.recentFailures = val;
            return this;
        }

        public Builder unusualHour(boolean val) {
            this.unusualHour = val;
            return this;
        }

        public RiskContext build() {
            return new RiskContext(
                    hasPreviousSuccessfulLogin,
                    trustedDevice,
                    recentSuccessFromSameDevice24h,
                    newDevice,
                    newNetwork,
                    recentFailures,
                    unusualHour
            );
        }
    }
}
