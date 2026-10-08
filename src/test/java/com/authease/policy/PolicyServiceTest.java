package com.authease.policy;

import com.authease.model.RiskLevel;
import com.authease.risk.RiskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class PolicyServiceTest {

    private PolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService = new PolicyService();
    }

    @Test
    void testFiveOrMoreFailures_ForcesHighEvenForTrustedDevice() {
        // Score is 0 (LOW), device is trusted (untrustedDevice = false)
        RiskResult lowRisk = new RiskResult(0, RiskLevel.LOW, Collections.emptyList());
        PolicyContext context = new PolicyContext(
                lowRisk,
                5,      // 5 failures
                false,  // trusted device
                false,  // mfa enabled
                false,  // admin
                0,      // account wide failures
                false   // sensitive action
        );

        PolicyDecision decision = policyService.applyPolicy(context);
        assertEquals(RiskLevel.HIGH, decision.getLevel());
        assertEquals(3, decision.getRequiredSteps());
        assertTrue(decision.isNeedsEmailApproval());
        assertEquals("MANY_FAILURES", decision.getReasonCode());
    }

    @Test
    void testUntrustedDeviceWithMfaEnabled_ForcesAtLeastMedium() {
        RiskResult lowRisk = new RiskResult(10, RiskLevel.LOW, Collections.emptyList());
        PolicyContext context = new PolicyContext(
                lowRisk,
                0,
                true,   // untrusted device
                true,   // mfa enabled
                false,
                0,
                false
        );

        PolicyDecision decision = policyService.applyPolicy(context);
        assertEquals(RiskLevel.MEDIUM, decision.getLevel());
        assertEquals(2, decision.getRequiredSteps());
        assertTrue(decision.getMethods().contains("TOTP"));
        assertTrue(decision.getMethods().contains("BACKUP_CODE"));
    }

    @Test
    void testAdminAccount_ForcesAtLeastMedium() {
        RiskResult lowRisk = new RiskResult(10, RiskLevel.LOW, Collections.emptyList());
        PolicyContext context = new PolicyContext(
                lowRisk,
                0,
                false,
                false,
                true,   // ADMIN
                0,
                false
        );

        PolicyDecision decision = policyService.applyPolicy(context);
        assertEquals(RiskLevel.MEDIUM, decision.getLevel());
        assertEquals(2, decision.getRequiredSteps());
    }

    @Test
    void testAccountWideFailures_AffectsUntrustedDeviceOnly() {
        RiskResult lowRisk = new RiskResult(10, RiskLevel.LOW, Collections.emptyList());

        // Attacker on untrusted device
        PolicyContext untrustedCtx = new PolicyContext(lowRisk, 0, true, false, false, 12, false);
        PolicyDecision untrustedDecision = policyService.applyPolicy(untrustedCtx);
        assertEquals(RiskLevel.MEDIUM, untrustedDecision.getLevel());
        assertTrue(untrustedDecision.isSuspiciousDetected());

        // Legitimate user on trusted device remains unaffected (LOW)
        PolicyContext trustedCtx = new PolicyContext(lowRisk, 0, false, false, false, 12, false);
        PolicyDecision trustedDecision = policyService.applyPolicy(trustedCtx);
        assertEquals(RiskLevel.LOW, trustedDecision.getLevel());
        assertTrue(trustedDecision.isSuspiciousDetected());
    }

    @Test
    void testStepCounts_Low1_Medium2_High3() {
        RiskResult low = new RiskResult(10, RiskLevel.LOW, Collections.emptyList());
        PolicyDecision decLow = policyService.applyPolicy(new PolicyContext(low, 0, false, false, false, 0, false));
        assertEquals(1, decLow.getRequiredSteps());

        RiskResult med = new RiskResult(40, RiskLevel.MEDIUM, Collections.emptyList());
        PolicyDecision decMed = policyService.applyPolicy(new PolicyContext(med, 0, false, false, false, 0, false));
        assertEquals(2, decMed.getRequiredSteps());

        RiskResult high = new RiskResult(80, RiskLevel.HIGH, Collections.emptyList());
        PolicyDecision decHigh = policyService.applyPolicy(new PolicyContext(high, 0, false, false, false, 0, false));
        assertEquals(3, decHigh.getRequiredSteps());
    }
}
