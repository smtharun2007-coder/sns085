package com.authease.dto;

import jakarta.validation.constraints.NotBlank;

public class SendEmailOtpRequest {

    @NotBlank(message = "Challenge ID is required")
    private String challengeId;

    public SendEmailOtpRequest() {}

    public SendEmailOtpRequest(String challengeId) {
        this.challengeId = challengeId;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }
}
