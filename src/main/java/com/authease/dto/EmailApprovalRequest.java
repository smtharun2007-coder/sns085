package com.authease.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class EmailApprovalRequest {

    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "Decision is required")
    @Pattern(regexp = "APPROVE|DENY", message = "Decision must be APPROVE or DENY")
    private String decision;

    public EmailApprovalRequest() {}

    public EmailApprovalRequest(String token, String decision) {
        this.token = token;
        this.decision = decision;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }
}
