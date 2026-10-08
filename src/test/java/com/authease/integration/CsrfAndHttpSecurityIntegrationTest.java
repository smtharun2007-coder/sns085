package com.authease.integration;

import com.authease.config.SecurityConfig;
import com.authease.controller.AuthController;
import com.authease.controller.GlobalExceptionHandler;
import com.authease.dto.GenericResponse;
import com.authease.security.ClientIpResolver;
import com.authease.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ReasonCatalogService.class, ClientIpResolver.class})
class CsrfAndHttpSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserService userService;

    @MockBean
    private ChallengeService challengeService;

    @MockBean
    private MfaService mfaService;

    @Test
    @DisplayName("Requirement 6: CSRF validation on state-modifying requests - rejected without CSRF token")
    void testPostWithoutCsrfTokenIsRejected() throws Exception {
        // Attempt POST /api/auth/register without CSRF token -> 403 Forbidden
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"displayName\":\"Test\",\"password\":\"Password123!\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Requirement 6: CSRF validation on state-modifying requests - accepted with CSRF token")
    void testPostWithCsrfTokenPassesCsrfCheck() throws Exception {
        when(userService.register(any(), any(), any())).thenReturn(GenericResponse.ok("Verification email sent"));

        // Attempt POST /api/auth/register with valid CSRF token -> 200 OK
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"displayName\":\"Test\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Requirement 9: Approval link consumes via POST only, not GET")
    void testEmailApprovalRejectsGetRequest() throws Exception {
        // Attempt GET /api/auth/email-approval -> 405 Method Not Allowed (prevents email scanner pre-fetch)
        mockMvc.perform(get("/api/auth/email-approval")
                        .param("token", "sample-approval-token"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("Requirement 9: Approval link succeeds via POST")
    void testEmailApprovalAcceptsPostRequest() throws Exception {
        when(mfaService.emailApproval(any())).thenReturn(GenericResponse.ok("Sign-in approved"));

        // Attempt POST /api/auth/email-approval with valid CSRF -> 200 OK
        mockMvc.perform(post("/api/auth/email-approval")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"valid-approval-token\",\"decision\":\"APPROVE\"}"))
                .andExpect(status().isOk());
    }
}
