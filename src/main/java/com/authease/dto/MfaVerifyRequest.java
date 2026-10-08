package com.authease.dto;

import jakarta.validation.constraints.NotBlank;

public class MfaVerifyRequest {

    @NotBlank(message = "Challenge ID is required")
    private String challengeId;

    @NotBlank(message = "Method is required")
    private String method; // TOTP, EMAIL_OTP, BACKUP_CODE

    @NotBlank(message = "Code is required")
    private String code;

    private Boolean rememberDevice = false;

    public MfaVerifyRequest() {}

    public MfaVerifyRequest(String challengeId, String method, String code, Boolean rememberDevice) {
        this.challengeId = challengeId;
        this.method = method;
        this.code = code;
        this.rememberDevice = rememberDevice != null ? rememberDevice : false;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Boolean getRememberDevice() {
        return rememberDevice;
    }

    public void setRememberDevice(Boolean rememberDevice) {
        this.rememberDevice = rememberDevice;
    }
}
