package com.authease.controller;

import com.authease.dto.AccountSecurityResponse;
import com.authease.dto.GenericResponse;
import com.authease.dto.LastDecisionResponse;
import com.authease.service.AccountService;
import com.authease.service.AuthService;
import com.authease.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/security")
    public ResponseEntity<AccountSecurityResponse> getSecurity(HttpServletRequest request) {
        String userId = getAuthenticatedUserIdOrThrow(request);
        String deviceCookie = extractDeviceCookie(request);
        return ResponseEntity.ok(accountService.getSecurityInfo(userId, deviceCookie));
    }

    @DeleteMapping("/devices/{id}")
    public ResponseEntity<GenericResponse> deleteDevice(@PathVariable String id, HttpServletRequest request) {
        String userId = getAuthenticatedUserIdOrThrow(request);
        accountService.deleteDevice(userId, id);
        return ResponseEntity.ok(GenericResponse.ok("Device revoked successfully"));
    }

    @GetMapping("/last-decision")
    public ResponseEntity<LastDecisionResponse> getLastDecision(HttpServletRequest request) {
        String userId = getAuthenticatedUserIdOrThrow(request);
        return ResponseEntity.ok(accountService.getLastDecision(userId));
    }

    private String getAuthenticatedUserIdOrThrow(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(AuthService.SESSION_USER_ID) == null) {
            throw new UserService.CustomAuthException(
                    "UNAUTHORIZED",
                    "Sign in required",
                    "You must be signed in to access account security.",
                    "Sign in with your account."
            );
        }
        return (String) session.getAttribute(AuthService.SESSION_USER_ID);
    }

    private String extractDeviceCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> AuthService.DEVICE_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
