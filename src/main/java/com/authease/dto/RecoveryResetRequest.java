package com.authease.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RecoveryResetRequest {

    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "Password is required")
    @Size(min = 10, max = 128, message = "Password must be between 10 and 128 characters")
    private String newPassword;

    private String secondFactorCode;

    public RecoveryResetRequest() {}

    public RecoveryResetRequest(String token, String newPassword, String secondFactorCode) {
        this.token = token;
        this.newPassword = newPassword;
        this.secondFactorCode = secondFactorCode;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getSecondFactorCode() {
        return secondFactorCode;
    }

    public void setSecondFactorCode(String secondFactorCode) {
        this.secondFactorCode = secondFactorCode;
    }
}
