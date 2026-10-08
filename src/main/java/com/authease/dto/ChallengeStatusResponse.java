package com.authease.dto;

public class ChallengeStatusResponse {

    private boolean emailApproved;
    private long expiresInSeconds;
    private long minimumDelayRemainingSeconds;

    public ChallengeStatusResponse() {}

    public ChallengeStatusResponse(boolean emailApproved, long expiresInSeconds, long minimumDelayRemainingSeconds) {
        this.emailApproved = emailApproved;
        this.expiresInSeconds = expiresInSeconds;
        this.minimumDelayRemainingSeconds = minimumDelayRemainingSeconds;
    }

    public boolean isEmailApproved() {
        return emailApproved;
    }

    public void setEmailApproved(boolean emailApproved) {
        this.emailApproved = emailApproved;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    public long getMinimumDelayRemainingSeconds() {
        return minimumDelayRemainingSeconds;
    }

    public void setMinimumDelayRemainingSeconds(long minimumDelayRemainingSeconds) {
        this.minimumDelayRemainingSeconds = minimumDelayRemainingSeconds;
    }
}
