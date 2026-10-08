package com.authease.controller;

import com.authease.dto.*;
import com.authease.security.ClientIpResolver;
import com.authease.service.AuthService;
import com.authease.service.ChallengeService;
import com.authease.service.MfaService;
import com.authease.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final ChallengeService challengeService;
    private final MfaService mfaService;
    private final ClientIpResolver clientIpResolver;

    public AuthController(AuthService authService,
                          UserService userService,
                          ChallengeService challengeService,
                          MfaService mfaService,
                          ClientIpResolver clientIpResolver) {
        this.authService = authService;
        this.userService = userService;
        this.challengeService = challengeService;
        this.mfaService = mfaService;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping("/me")
    public ResponseEntity<AuthMeResponse> me(HttpServletRequest request) {
        return ResponseEntity.ok(authService.me(request));
    }

    @PostMapping("/register")
    public ResponseEntity<GenericResponse> register(@Valid @RequestBody RegisterRequest request,
                                                    HttpServletRequest httpRequest) {
        String clientIp = clientIpResolver.resolveClientIp(httpRequest);
        String demoSessionId = resolveDemoSessionId(httpRequest);
        return ResponseEntity.ok(userService.register(request, demoSessionId, clientIp));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<GenericResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request,
                                                       HttpServletRequest httpRequest) {
        String clientIp = clientIpResolver.resolveClientIp(httpRequest);
        return ResponseEntity.ok(userService.verifyEmail(request.getToken(), clientIp));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<GenericResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request,
                                                              HttpServletRequest httpRequest) {
        String clientIp = clientIpResolver.resolveClientIp(httpRequest);
        String demoSessionId = resolveDemoSessionId(httpRequest);
        return ResponseEntity.ok(userService.resendVerification(request.getEmail(), demoSessionId, clientIp));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, httpRequest));
    }

    @PostMapping("/mfa/send-email-otp")
    public ResponseEntity<GenericResponse> sendEmailOtp(@Valid @RequestBody SendEmailOtpRequest request,
                                                        HttpServletRequest httpRequest) {
        return ResponseEntity.ok(mfaService.sendEmailOtp(request.getChallengeId(), httpRequest));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<LoginResponse> verifyMfa(@Valid @RequestBody MfaVerifyRequest request,
                                                   HttpServletRequest httpRequest,
                                                   HttpServletResponse httpResponse) {
        return ResponseEntity.ok(mfaService.verifyMfa(request, httpRequest, httpResponse));
    }

    @GetMapping("/challenge/{challengeId}/status")
    public ResponseEntity<ChallengeStatusResponse> getChallengeStatus(@PathVariable String challengeId,
                                                                      HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        String boundSessionId = session != null ? session.getId() : "unbound";
        return ResponseEntity.ok(challengeService.getStatus(challengeId, boundSessionId));
    }

    @PostMapping("/email-approval")
    public ResponseEntity<GenericResponse> emailApproval(@Valid @RequestBody EmailApprovalRequest request) {
        return ResponseEntity.ok(mfaService.emailApproval(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<GenericResponse> logout(HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.logout(httpRequest));
    }

    @PostMapping("/session/extend")
    public ResponseEntity<GenericResponse> extendSession(HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.extendSession(httpRequest));
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
