package com.authease.dto;

import java.util.List;
import java.util.Map;

public class AdminMetricsResponse {

    public static class HourlyFailure {
        private String hour;
        private long count;

        public HourlyFailure() {}

        public HourlyFailure(String hour, long count) {
            this.hour = hour;
            this.count = count;
        }

        public String getHour() {
            return hour;
        }

        public void setHour(String hour) {
            this.hour = hour;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    private double firstTrySuccessRate;
    private double mfaDropOffRate;
    private double recoveryCompletionRate;
    private long avgRecoverySeconds;
    private Map<String, Long> loginsByLevel;
    private List<HourlyFailure> failuresPerHour;

    public AdminMetricsResponse() {}

    public AdminMetricsResponse(double firstTrySuccessRate, double mfaDropOffRate,
                                double recoveryCompletionRate, long avgRecoverySeconds,
                                Map<String, Long> loginsByLevel, List<HourlyFailure> failuresPerHour) {
        this.firstTrySuccessRate = firstTrySuccessRate;
        this.mfaDropOffRate = mfaDropOffRate;
        this.recoveryCompletionRate = recoveryCompletionRate;
        this.avgRecoverySeconds = avgRecoverySeconds;
        this.loginsByLevel = loginsByLevel;
        this.failuresPerHour = failuresPerHour;
    }

    public double getFirstTrySuccessRate() {
        return firstTrySuccessRate;
    }

    public void setFirstTrySuccessRate(double firstTrySuccessRate) {
        this.firstTrySuccessRate = firstTrySuccessRate;
    }

    public double getMfaDropOffRate() {
        return mfaDropOffRate;
    }

    public void setMfaDropOffRate(double mfaDropOffRate) {
        this.mfaDropOffRate = mfaDropOffRate;
    }

    public double getRecoveryCompletionRate() {
        return recoveryCompletionRate;
    }

    public void setRecoveryCompletionRate(double recoveryCompletionRate) {
        this.recoveryCompletionRate = recoveryCompletionRate;
    }

    public long getAvgRecoverySeconds() {
        return avgRecoverySeconds;
    }

    public void setAvgRecoverySeconds(long avgRecoverySeconds) {
        this.avgRecoverySeconds = avgRecoverySeconds;
    }

    public Map<String, Long> getLoginsByLevel() {
        return loginsByLevel;
    }

    public void setLoginsByLevel(Map<String, Long> loginsByLevel) {
        this.loginsByLevel = loginsByLevel;
    }

    public List<HourlyFailure> getFailuresPerHour() {
        return failuresPerHour;
    }

    public void setFailuresPerHour(List<HourlyFailure> failuresPerHour) {
        this.failuresPerHour = failuresPerHour;
    }
}
