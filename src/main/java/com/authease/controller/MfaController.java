package com.authease.controller;

import com.authease.dto.BackupCodesResponse;
import com.authease.dto.TotpConfirmRequest;
import com.authease.dto.TotpSetupResponse;
import com.authease.service.AuthService;
import com.authease.service.MfaService;
import com.authease.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mfa")
public class MfaController {

    private final MfaService mfaService;

    public MfaController(MfaService mfaService) {
        this.mfaService = mfaService;
    }

    @PostMapping("/totp/setup")
    public ResponseEntity<TotpSetupResponse> setupTotp(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(AuthService.SESSION_USER_ID) == null) {
            throw new UserService.CustomAuthException("UNAUTHORIZED", "Sign in required", "Please sign in.", "Sign in first.");
        }
        String userId = (String) session.getAttribute(AuthService.SESSION_USER_ID);
        TotpSetupResponse resp = mfaService.setupTotp(userId);
        session.setAttribute("PENDING_TOTP_SECRET", resp.getManualKey());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/totp/confirm")
    public ResponseEntity<BackupCodesResponse> confirmTotp(@Valid @RequestBody TotpConfirmRequest req,
                                                           HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(AuthService.SESSION_USER_ID) == null) {
            throw new UserService.CustomAuthException("UNAUTHORIZED", "Sign in required", "Please sign in.", "Sign in first.");
        }
        String userId = (String) session.getAttribute(AuthService.SESSION_USER_ID);
        String pendingSecret = (String) session.getAttribute("PENDING_TOTP_SECRET");
        if (pendingSecret == null) {
            throw new UserService.CustomAuthException("MFA_NOT_SET_UP", "Setup expired", "Please restart setup.", "Start TOTP setup again.");
        }
        BackupCodesResponse resp = mfaService.confirmTotp(userId, pendingSecret, req.getCode());
        session.removeAttribute("PENDING_TOTP_SECRET");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/backup-codes/regenerate")
    public ResponseEntity<BackupCodesResponse> regenerateBackupCodes(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(AuthService.SESSION_USER_ID) == null) {
            throw new UserService.CustomAuthException("UNAUTHORIZED", "Sign in required", "Please sign in.", "Sign in first.");
        }
        String userId = (String) session.getAttribute(AuthService.SESSION_USER_ID);
        return ResponseEntity.ok(mfaService.regenerateBackupCodes(userId));
    }
}
