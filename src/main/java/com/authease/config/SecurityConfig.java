package com.authease.config;

import com.authease.dto.ErrorResponse;
import com.authease.security.CsrfCookieFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        // Null attribute name allows raw token comparison from header matching cookie
        requestHandler.setCsrfRequestAttributeName(null);

        http
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation().migrateSession()
            )
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(requestHandler)
                .ignoringRequestMatchers("/api/health") // health endpoint doesn't require CSRF
            )
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; font-src 'self'; frame-ancestors 'none';")
                )
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(contentType -> {})
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
            )
            .authorizeHttpRequests(auth -> auth
                // Static UI files and routes
                .requestMatchers(
                    "/", "/*.html", "/**.html",
                    "/help.html", "/dev-outbox.html", "/account.html", "/recover.html",
                    "/index.html", "/login.html", "/register.html",
                    "/check-email.html", "/verify-email.html", "/approve.html",
                    "/mfa-setup.html",
                    "/help", "/dev-outbox", "/account", "/recover", "/login", "/register",
                    "/css/**", "/js/**", "/vendor/**", "/favicon.ico",
                    "/*.ico", "/*.png", "/*.svg"
                ).permitAll()
                // Public & Controller-guarded APIs
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/mfa/**").permitAll()
                .requestMatchers("/api/password/check").permitAll()
                .requestMatchers("/api/recovery/**").permitAll()
                .requestMatchers("/api/dev/**").permitAll()
                .requestMatchers("/api/demo/**").permitAll()
                .requestMatchers("/api/assist/**").permitAll()
                .requestMatchers("/api/admin/**").permitAll()
                .requestMatchers("/api/account/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    ErrorResponse err = new ErrorResponse(
                        "UNAUTHORIZED",
                        "Sign in required",
                        "You must be signed in to access this feature.",
                        "Please sign in with your account credentials."
                    );
                    objectMapper.writeValue(response.getOutputStream(), err);
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    ErrorResponse err = new ErrorResponse(
                        "FORBIDDEN",
                        "Access Denied",
                        "You do not have permission to access this resource or a valid CSRF token was missing.",
                        "Please refresh the page and try again."
                    );
                    objectMapper.writeValue(response.getOutputStream(), err);
                })
            );

        return http.build();
    }
}
