package com.authease.controller;

import com.authease.dto.AdminEventsResponse;
import com.authease.dto.AdminMetricsResponse;
import com.authease.model.LoginEvent;
import com.authease.model.LoginEventType;
import com.authease.model.RiskLevel;
import com.authease.model.User;
import com.authease.repository.UserRepository;
import com.authease.service.AuthService;
import com.authease.service.MetricsService;
import com.authease.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final MetricsService metricsService;
    private final UserRepository userRepository;

    public AdminController(MetricsService metricsService, UserRepository userRepository) {
        this.metricsService = metricsService;
        this.userRepository = userRepository;
    }

    @GetMapping("/metrics")
    public ResponseEntity<AdminMetricsResponse> getMetrics(
            @RequestParam(defaultValue = "24h") String range,
            HttpServletRequest request) {
        checkAdminAccess(request);
        AdminMetricsResponse metrics = metricsService.getMetrics(range);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/events")
    public ResponseEntity<AdminEventsResponse> getEvents(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request) {
        checkAdminAccess(request);

        LoginEventType eventType = null;
        if (type != null && !type.isBlank()) {
            try {
                eventType = LoginEventType.valueOf(type.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        RiskLevel riskLevel = null;
        if (level != null && !level.isBlank()) {
            try {
                riskLevel = RiskLevel.valueOf(level.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        int targetPage = Math.max(0, page - 1);
        int targetSize = Math.max(1, Math.min(100, pageSize));

        PageRequest pageRequest = PageRequest.of(targetPage, targetSize, Sort.by(Sort.Direction.DESC, "ts"));
        Page<LoginEvent> resultPage = metricsService.getEvents(eventType, riskLevel, pageRequest);

        AdminEventsResponse response = new AdminEventsResponse(
                resultPage.getContent(),
                resultPage.getTotalElements(),
                page,
                targetSize
        );

        return ResponseEntity.ok(response);
    }

    private void checkAdminAccess(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(AuthService.SESSION_USER_ID) == null) {
            throw new UserService.CustomAuthException(
                    "UNAUTHORIZED",
                    "Sign in required",
                    "You must be signed in to access the administrator panel.",
                    "Sign in with an administrator account."
            );
        }

        String userId = (String) session.getAttribute(AuthService.SESSION_USER_ID);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserService.CustomAuthException(
                        "UNAUTHORIZED",
                        "User not found",
                        "Account could not be found.",
                        "Sign in again."
                ));

        boolean isAdmin = user.getRoles() != null && user.getRoles().contains("ADMIN");
        if (!isAdmin) {
            throw new UserService.CustomAuthException(
                    "FORBIDDEN",
                    "Admin access restricted",
                    "You do not have the required administrative role for this resource.",
                    "Contact an administrator if you need access."
            );
        }

        // Defense-in-depth: Admin MUST have TOTP MFA active
        if (!user.isTotpEnabled()) {
            throw new UserService.CustomAuthException(
                    "ADMIN_MFA_REQUIRED",
                    "Two-factor authentication required for administrators",
                    "Administrative accounts must have two-factor authentication enabled to access the admin portal.",
                    "Set up an authenticator app in your security settings to proceed."
            );
        }
    }
}
