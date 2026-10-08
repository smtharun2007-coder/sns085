package com.authease.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class RecoveryRequestDto {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address format")
    private String email;

    public RecoveryRequestDto() {}

    public RecoveryRequestDto(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
