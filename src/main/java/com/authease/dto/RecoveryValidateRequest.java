package com.authease.dto;

import jakarta.validation.constraints.NotBlank;

public class RecoveryValidateRequest {

    @NotBlank(message = "Token is required")
    private String token;

    public RecoveryValidateRequest() {}

    public RecoveryValidateRequest(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
