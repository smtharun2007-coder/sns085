package com.authease.controller;

import com.authease.dto.PasswordCheckRequest;
import com.authease.dto.PasswordCheckResponse;
import com.authease.service.PasswordPolicyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/password")
public class PasswordController {

    private final PasswordPolicyService passwordPolicyService;

    public PasswordController(PasswordPolicyService passwordPolicyService) {
        this.passwordPolicyService = passwordPolicyService;
    }

    @PostMapping("/check")
    public ResponseEntity<PasswordCheckResponse> checkPassword(@Valid @RequestBody PasswordCheckRequest request) {
        return ResponseEntity.ok(passwordPolicyService.evaluatePassword(request.getPassword()));
    }
}
