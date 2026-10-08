package com.authease.controller;

import com.authease.dto.*;
import com.authease.security.ClientIpResolver;
import com.authease.service.RecoveryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recovery")
public class RecoveryController {

    private final RecoveryService recoveryService;
    private final ClientIpResolver clientIpResolver;

    public RecoveryController(RecoveryService recoveryService,
                              ClientIpResolver clientIpResolver) {
        this.recoveryService = recoveryService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/request")
    public ResponseEntity<GenericResponse> requestRecovery(@Valid @RequestBody RecoveryRequestDto request,
                                                           HttpServletRequest httpRequest) {
        String clientIp = clientIpResolver.resolveClientIp(httpRequest);
        String demoSessionId = resolveDemoSessionId(httpRequest);
        return ResponseEntity.ok(recoveryService.requestRecovery(request, demoSessionId, clientIp));
    }

    @PostMapping("/validate")
    public ResponseEntity<RecoveryValidateResponse> validateToken(@Valid @RequestBody RecoveryValidateRequest request) {
        return ResponseEntity.ok(recoveryService.validateToken(request.getToken()));
    }

    @PostMapping("/reset")
    public ResponseEntity<GenericResponse> resetPassword(@Valid @RequestBody RecoveryResetRequest request,
                                                         HttpServletRequest httpRequest) {
        return ResponseEntity.ok(recoveryService.resetPassword(request, httpRequest));
    }

    private String resolveDemoSessionId(HttpServletRequest request) {
        String headerSession = request.getHeader("X-Demo-Session");
        if (headerSession != null && !headerSession.isBlank()) {
            return headerSession.trim();
        }
        HttpSession session = request.getSession(false);
        return session != null ? session.getId() : "default";
    }
}
