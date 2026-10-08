package com.authease.controller;

import com.authease.dto.AdminMetricsResponse;
import com.authease.model.User;
import com.authease.repository.UserRepository;
import com.authease.service.AuthService;
import com.authease.service.MetricsService;
import com.authease.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminControllerTest {

    private MetricsService metricsService;
    private UserRepository userRepository;
    private AdminController adminController;
    private HttpServletRequest request;
    private HttpSession session;

    @BeforeEach
    void setUp() {
        metricsService = Mockito.mock(MetricsService.class);
        userRepository = Mockito.mock(UserRepository.class);
        adminController = new AdminController(metricsService, userRepository);
        request = Mockito.mock(HttpServletRequest.class);
        session = Mockito.mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
    }

    @Test
    void testUnauthenticatedThrowsUnauthorized() {
        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(null);

        UserService.CustomAuthException ex = assertThrows(
                UserService.CustomAuthException.class,
                () -> adminController.getMetrics("24h", request)
        );
        assertEquals("UNAUTHORIZED", ex.getReasonCode());
    }

    @Test
    void testNonAdminThrowsForbidden() {
        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn("user-1");
        User standardUser = new User();
        standardUser.setId("user-1");
        standardUser.setRoles(Set.of("USER"));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(standardUser));

        UserService.CustomAuthException ex = assertThrows(
                UserService.CustomAuthException.class,
                () -> adminController.getMetrics("24h", request)
        );
        assertEquals("FORBIDDEN", ex.getReasonCode());
    }

    @Test
    void testAdminWithoutTotpThrowsAdminMfaRequired() {
        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn("admin-1");
        User adminUser = new User();
        adminUser.setId("admin-1");
        adminUser.setRoles(Set.of("ADMIN"));
        adminUser.setTotpEnabled(false); // TOTP disabled
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(adminUser));

        UserService.CustomAuthException ex = assertThrows(
                UserService.CustomAuthException.class,
                () -> adminController.getMetrics("24h", request)
        );
        assertEquals("ADMIN_MFA_REQUIRED", ex.getReasonCode());
    }

    @Test
    void testAdminWithTotpSucceeds() {
        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn("admin-1");
        User adminUser = new User();
        adminUser.setId("admin-1");
        adminUser.setRoles(Set.of("ADMIN"));
        adminUser.setTotpEnabled(true);
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(adminUser));

        AdminMetricsResponse metrics = new AdminMetricsResponse(95.0, 2.0, 100.0, 114L, Map.of(), List.of());
        when(metricsService.getMetrics("24h")).thenReturn(metrics);

        ResponseEntity<AdminMetricsResponse> response = adminController.getMetrics("24h", request);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(95.0, response.getBody().getFirstTrySuccessRate());
    }
}
