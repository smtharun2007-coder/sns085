package com.authease.service;

import com.authease.dto.AuthMeResponse;
import com.authease.dto.GenericResponse;
import com.authease.dto.LoginRequest;
import com.authease.dto.LoginResponse;
import com.authease.model.*;
import com.authease.policy.PolicyContext;
import com.authease.policy.PolicyDecision;
import com.authease.policy.PolicyService;
import com.authease.repository.LoginEventRepository;
import com.authease.repository.TrustedDeviceRepository;
import com.authease.repository.UserRepository;
import com.authease.risk.RiskContext;
import com.authease.risk.RiskEngine;
import com.authease.risk.RiskResult;
import com.authease.security.Argon2SecurityUtil;
import com.authease.security.ClientIpResolver;
import com.authease.security.RateLimitService;
import com.authease.util.CryptoUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AuthService {

    public static final String SESSION_USER_ID = "AUTH_USER_ID";
    public static final String DEVICE_COOKIE_NAME = "ae_device";

    private final UserRepository userRepository;
    private final TrustedDeviceRepository trustedDeviceRepository;
    private final LoginEventRepository loginEventRepository;
    private final Argon2SecurityUtil passwordEncoder;
    private final RateLimitService rateLimitService;
    private final ClientIpResolver clientIpResolver;
    private final AuditService auditService;
    private final EmailService emailService;
    private final ReasonCatalogService reasonCatalogService;
    private final RiskEngine riskEngine;
    private final PolicyService policyService;
    private final ChallengeService challengeService;
    private final DemoContextService demoContextService;

    public AuthService(UserRepository userRepository,
                       TrustedDeviceRepository trustedDeviceRepository,
                       LoginEventRepository loginEventRepository,
                       Argon2SecurityUtil passwordEncoder,
                       RateLimitService rateLimitService,
                       ClientIpResolver clientIpResolver,
                       AuditService auditService,
                       EmailService emailService,
                       ReasonCatalogService reasonCatalogService,
                       RiskEngine riskEngine,
                       PolicyService policyService,
                       ChallengeService challengeService) {
        this(userRepository, trustedDeviceRepository, loginEventRepository, passwordEncoder,
                rateLimitService, clientIpResolver, auditService, emailService,
                reasonCatalogService, riskEngine, policyService, challengeService, null);
    }

    @Autowired
    public AuthService(UserRepository userRepository,
                       TrustedDeviceRepository trustedDeviceRepository,
                       LoginEventRepository loginEventRepository,
                       Argon2SecurityUtil passwordEncoder,
                       RateLimitService rateLimitService,
                       ClientIpResolver clientIpResolver,
                       AuditService auditService,
                       EmailService emailService,
                       ReasonCatalogService reasonCatalogService,
                       RiskEngine riskEngine,
                       PolicyService policyService,
                       ChallengeService challengeService,
                       DemoContextService demoContextService) {
        this.userRepository = userRepository;
        this.trustedDeviceRepository = trustedDeviceRepository;
        this.loginEventRepository = loginEventRepository;
        this.passwordEncoder = passwordEncoder;
        this.rateLimitService = rateLimitService;
        this.clientIpResolver = clientIpResolver;
        this.auditService = auditService;
        this.emailService = emailService;
        this.reasonCatalogService = reasonCatalogService;
        this.riskEngine = riskEngine;
        this.policyService = policyService;
        this.challengeService = challengeService;
        this.demoContextService = demoContextService;
    }

    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = clientIpResolver.resolveClientIp(httpRequest);
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        String rawPassword = request.getPassword() != null ? request.getPassword() : "";

        // 1. Check Rate Limit for key(ip, identifier)
        RateLimitService.RateLimitStatus rateStatus = rateLimitService.checkRateLimit(clientIp, email);
        if (rateStatus.isDelayed()) {
            ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("RATE_DELAY");
            return LoginResponse.delayed(rateStatus.getRetryAfterSeconds(), detail.reasonCode(),
                    detail.title(), detail.message(), detail.nextStep());
        }

        // 2. Verify password with timing defense
        Optional<User> optionalUser = userRepository.findByEmail(email);
        boolean passwordValid;
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            passwordValid = passwordEncoder.matches(rawPassword, user.getPasswordHash());
        } else {
            passwordEncoder.verifyAgainstDummyHash(rawPassword);
            passwordValid = false;
        }

        String ipPrefixHash = clientIpResolver.getNetworkPrefixHash(clientIp);

        if (!passwordValid) {
            rateLimitService.recordFailure(clientIp, email);
            auditService.logEvent(optionalUser.map(User::getId).orElse(null), email,
                    LoginEventType.LOGIN_FAILURE, RiskLevel.LOW, null, null,
                    "INVALID_CREDENTIALS", ipPrefixHash, null);

            ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("INVALID_CREDENTIALS");
            return LoginResponse.failed(detail.reasonCode(), detail.title(), detail.message(), detail.nextStep());
        }

        User user = optionalUser.get();

        // Check email verification status
        if (!user.isEmailVerified()) {
            ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get("EMAIL_NOT_VERIFIED");
            return LoginResponse.failed(detail.reasonCode(), detail.title(), detail.message(), detail.nextStep());
        }

        // 3. Inspect device trust from cookie "ae_device"
        String rawDeviceCookie = extractDeviceCookie(httpRequest);
        boolean trustedDevice = false;
        boolean recentSuccessFromSameDevice = false;
        String deviceId = null;

        if (rawDeviceCookie != null && !rawDeviceCookie.isBlank()) {
            String tokenHash = CryptoUtil.sha256(rawDeviceCookie);
            Optional<TrustedDevice> devOpt = trustedDeviceRepository.findByUserIdAndTokenHash(user.getId(), tokenHash);
            if (devOpt.isPresent()) {
                TrustedDevice dev = devOpt.get();
                if (dev.getExpiresAt() != null && dev.getExpiresAt().isAfter(Instant.now())) {
                    trustedDevice = true;
                    deviceId = dev.getId();
                    if (dev.getLastUsedAt() != null && dev.getLastUsedAt().isAfter(Instant.now().minus(Duration.ofHours(24)))) {
                        recentSuccessFromSameDevice = true;
                    }
                    dev.setLastUsedAt(Instant.now());
                    trustedDeviceRepository.save(dev);
                }
            }
        }

        // 4. Build Risk Signals
        boolean hasPrevSuccess = user.getLastSuccessfulLoginAt() != null;
        boolean newDevice = !trustedDevice;
        boolean newNetwork = user.getKnownNetworkHashes() == null || !user.getKnownNetworkHashes().contains(ipPrefixHash);
        int recentFailures = rateLimitService.getRecentFailures(clientIp, email);
        boolean unusualHour = isUnusualHour(user);

        // Apply Demo Mode simulation overrides if active
        if (demoContextService != null && demoContextService.isDemoMode()) {
            HttpSession session = httpRequest.getSession(false);
            if (session != null) {
                com.authease.dto.DemoContextDto demo = demoContextService.consumeNextContext(session.getId());
                if (demo != null) {
                    if (Boolean.TRUE.equals(demo.getNewDevice())) {
                        newDevice = true;
                        trustedDevice = false;
                        hasPrevSuccess = true;
                    } else if (Boolean.FALSE.equals(demo.getNewDevice())) {
                        newDevice = false;
                        trustedDevice = true;
                    }
                    if (Boolean.TRUE.equals(demo.getNewNetwork())) {
                        newNetwork = true;
                        hasPrevSuccess = true;
                    } else if (Boolean.FALSE.equals(demo.getNewNetwork())) {
                        newNetwork = false;
                    }
                    if (Boolean.TRUE.equals(demo.getUnusualHour())) {
                        unusualHour = true;
                    } else if (Boolean.FALSE.equals(demo.getUnusualHour())) {
                        unusualHour = false;
                    }
                    if (demo.getHourOfDay() != null) {
                        unusualHour = isUnusualHourGiven(user, demo.getHourOfDay());
                    }
                    if (demo.resolveEffectiveFailures() > 0) {
                        recentFailures = Math.max(recentFailures, demo.resolveEffectiveFailures());
                    }
                    if (Boolean.TRUE.equals(demo.getHighRiskFlag())) {
                        hasPrevSuccess = true;
                        newDevice = true;
                        newNetwork = true;
                        unusualHour = true;
                        recentFailures = Math.max(recentFailures, 3);
                        trustedDevice = false;
                    }
                }
            }
        }

        RiskContext riskContext = RiskContext.builder()
                .hasPreviousSuccessfulLogin(hasPrevSuccess)
                .trustedDevice(trustedDevice)
                .recentSuccessFromSameDevice24h(recentSuccessFromSameDevice)
                .newDevice(newDevice)
                .newNetwork(newNetwork)
                .recentFailures(recentFailures)
                .unusualHour(unusualHour)
                .build();

        RiskResult riskResult = riskEngine.evaluate(riskContext);

        // Account-wide failures check (last 15m)
        long accountWideFailures = 0;
        try {
            String identifierHash = CryptoUtil.sha256(email);
            accountWideFailures = loginEventRepository.countByIdentifierHashAndTypeAndTsAfter(
                    identifierHash, LoginEventType.LOGIN_FAILURE, Instant.now().minus(Duration.ofMinutes(15)));
        } catch (Exception ignored) {}

        boolean isAdmin = user.getRoles() != null && user.getRoles().contains("ADMIN");

        // 5. Policy Service evaluation
        PolicyContext policyContext = new PolicyContext(
                riskResult,
                recentFailures,
                !trustedDevice,
                user.isTotpEnabled(),
                isAdmin,
                accountWideFailures,
                false
        );

        PolicyDecision policyDecision = policyService.applyPolicy(policyContext);

        // Handle Suspicious Activity detection
        if (policyDecision.isSuspiciousDetected()) {
            auditService.logEvent(user.getId(), email, LoginEventType.SUSPICIOUS_DETECTED,
                    policyDecision.getLevel(), riskResult.getScore(),
                    List.of("ACCOUNT_WIDE_FAILURES"), "MANY_FAILURES", ipPrefixHash, deviceId);
            emailService.sendEmail(httpRequest.getSession(true).getId(), user.getEmail(),
                    "Security Alert: Multiple failed sign-in attempts",
                    "We noticed multiple failed sign-in attempts on your account. If this was not you, please secure your account.");
        }

        // 6. Handle LOW (password authenticated)
        if (policyDecision.getLevel() == RiskLevel.LOW) {
            rateLimitService.clear(clientIp, email);
            updateUserLoginHistory(user, ipPrefixHash);
            createAuthenticatedSession(user, httpRequest);

            auditService.logEvent(user.getId(), email, LoginEventType.LOGIN_SUCCESS,
                    RiskLevel.LOW, riskResult.getScore(), List.of("PASSWORD_VALID"),
                    "LOGIN_SUCCESS", ipPrefixHash, deviceId);

            return LoginResponse.authenticated();
        }

        // 7. Handle MEDIUM and HIGH (create challenge bound to current session)
        HttpSession session = httpRequest.getSession(true);
        ChallengeService.ChallengeCreation creation = challengeService.createChallenge(
                user.getId(), session.getId(), policyDecision.getLevel(), policyDecision.getRequiredSteps());
        Challenge challenge = creation.challenge();

        // If HIGH: send email approval and alert email
        if (policyDecision.getLevel() == RiskLevel.HIGH) {
            if (creation.rawApprovalToken() != null) {
                emailService.sendEmailApproval(session.getId(), user.getEmail(), creation.rawApprovalToken());
            }
            emailService.sendEmail(session.getId(), user.getEmail(),
                    "High-risk sign-in alert - AuthEase",
                    "A high-risk sign-in attempt was detected. Approval via email is required to complete this sign in.");
        }

        // Log MFA_SENT audit event
        List<String> signalCodes = riskResult.getSignals().stream().map(s -> s.getCode()).collect(Collectors.toList());
        auditService.logEvent(user.getId(), email, LoginEventType.MFA_SENT,
                policyDecision.getLevel(), riskResult.getScore(), signalCodes,
                policyDecision.getReasonCode(), ipPrefixHash, deviceId);

        ReasonCatalogService.ReasonDetail reasonDetail = reasonCatalogService.get(policyDecision.getReasonCode());

        return LoginResponse.mfaRequired(
                challenge.getId(),
                policyDecision.getLevel().name(),
                policyDecision.getMethods(),
                policyDecision.isNeedsEmailApproval(),
                1, // current step is 1 (password verified)
                policyDecision.getRequiredSteps(),
                reasonDetail.reasonCode(),
                reasonDetail.title(),
                reasonDetail.message(),
                reasonDetail.nextStep()
        );
    }

    public void createAuthenticatedSession(User user, HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(true);
        httpRequest.changeSessionId();
        session.setAttribute(SESSION_USER_ID, user.getId());

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toList());

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);

        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }

    public AuthMeResponse me(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            return AuthMeResponse.unauthenticated();
        }

        String userId = (String) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return AuthMeResponse.unauthenticated();
        }

        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            session.invalidate();
            return AuthMeResponse.unauthenticated();
        }

        User user = optionalUser.get();
        AuthMeResponse.UserSummary summary = new AuthMeResponse.UserSummary(
                user.getEmail(),
                user.getDisplayName(),
                user.getRoles(),
                user.isTotpEnabled()
        );

        long maxInactive = session.getMaxInactiveInterval();
        return AuthMeResponse.authenticated(summary, maxInactive > 0 ? maxInactive : 1800L);
    }

    public GenericResponse logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return GenericResponse.ok("Logged out successfully");
    }

    public GenericResponse extendSession(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.setMaxInactiveInterval(1800);
        }
        return GenericResponse.ok("Session extended");
    }

    private String extractDeviceCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> DEVICE_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private boolean isUnusualHour(User user) {
        int currentHour = ZonedDateTime.now(ZoneOffset.UTC).getHour();
        return isUnusualHourGiven(user, currentHour);
    }

    private boolean isUnusualHourGiven(User user, int hour) {
        List<Integer> history = user.getLoginHours();
        if (history == null || history.size() < 5) {
            // with fewer than 5 logins treat 00:00-05:00 as unusual
            return hour >= 0 && hour <= 5;
        }
        return !history.contains(hour);
    }

    private void updateUserLoginHistory(User user, String ipPrefixHash) {
        user.setLastSuccessfulLoginAt(Instant.now());

        int currentHour = ZonedDateTime.now(ZoneOffset.UTC).getHour();
        List<Integer> hours = user.getLoginHours() != null ? new ArrayList<>(user.getLoginHours()) : new ArrayList<>();
        hours.add(currentHour);
        if (hours.size() > 20) {
            hours = hours.subList(hours.size() - 20, hours.size());
        }
        user.setLoginHours(hours);

        List<String> networks = user.getKnownNetworkHashes() != null ? new ArrayList<>(user.getKnownNetworkHashes()) : new ArrayList<>();
        if (!networks.contains(ipPrefixHash)) {
            networks.add(ipPrefixHash);
            if (networks.size() > 10) {
                networks = networks.subList(networks.size() - 10, networks.size());
            }
            user.setKnownNetworkHashes(networks);
        }

        userRepository.save(user);
    }
}
