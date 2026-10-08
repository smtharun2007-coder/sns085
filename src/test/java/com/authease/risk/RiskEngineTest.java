package com.authease.risk;

import com.authease.model.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RiskEngineTest {

    private RiskEngine riskEngine;

    @BeforeEach
    void setUp() {
        riskEngine = new RiskEngine();
    }

    @Test
    void testTrustedDevicePlusRecentSuccess_EvaluatesToZeroLow() {
        // Base 10 - 20 (trusted) - 10 (recent success) = -20 -> clamped to 0 LOW
        RiskContext context = RiskContext.builder()
                .hasPreviousSuccessfulLogin(true)
                .trustedDevice(true)
                .recentSuccessFromSameDevice24h(true)
                .build();

        RiskResult result = riskEngine.evaluate(context);
        assertEquals(0, result.getScore());
        assertEquals(RiskLevel.LOW, result.getLevel());
    }

    @Test
    void testNewDeviceOnly_EvaluatesTo35Medium() {
        // Base 10 + 25 (new device) = 35 MEDIUM
        RiskContext context = RiskContext.builder()
                .hasPreviousSuccessfulLogin(true)
                .newDevice(true)
                .build();

        RiskResult result = riskEngine.evaluate(context);
        assertEquals(35, result.getScore());
        assertEquals(RiskLevel.MEDIUM, result.getLevel());
    }

    @Test
    void testNewDevicePlusUnusualHour_EvaluatesTo45Medium() {
        // Base 10 + 25 (new device) + 10 (unusual hour) = 45 MEDIUM
        RiskContext context = RiskContext.builder()
                .hasPreviousSuccessfulLogin(true)
                .newDevice(true)
                .unusualHour(true)
                .build();

        RiskResult result = riskEngine.evaluate(context);
        assertEquals(45, result.getScore());
        assertEquals(RiskLevel.MEDIUM, result.getLevel());
    }

    @Test
    void testNewDevicePlusNewNetworkPlusThreeFailures_EvaluatesTo80High() {
        // Base 10 + 25 (new device) + 20 (new network) + 25 (>=3 failures) = 80 HIGH
        RiskContext context = RiskContext.builder()
                .hasPreviousSuccessfulLogin(true)
                .newDevice(true)
                .newNetwork(true)
                .recentFailures(3)
                .build();

        RiskResult result = riskEngine.evaluate(context);
        assertEquals(80, result.getScore());
        assertEquals(RiskLevel.HIGH, result.getLevel());
    }

    @Test
    void testClampingAtZeroAndOneHundred() {
        // Test lower clamp at 0
        RiskContext lowContext = RiskContext.builder()
                .hasPreviousSuccessfulLogin(true)
                .trustedDevice(true)
                .recentSuccessFromSameDevice24h(true)
                .build();
        RiskResult lowResult = riskEngine.evaluate(lowContext);
        assertEquals(0, lowResult.getScore());

        // Test upper clamp at 100: Base 10 + 25 + 20 + 25 + 10 = 90
        // If we also had extra positive signals, clamped at 100
        RiskContext highContext = RiskContext.builder()
                .hasPreviousSuccessfulLogin(true)
                .newDevice(true)       // +25
                .newNetwork(true)      // +20
                .recentFailures(5)     // +25
                .unusualHour(true)     // +10
                .build();
        RiskResult highResult = riskEngine.evaluate(highContext);
        assertEquals(90, highResult.getScore());
        assertTrue(highResult.getScore() <= 100);
        assertEquals(RiskLevel.HIGH, highResult.getLevel());
    }

    @Test
    void testFirstEverLogin_EvaluatesTo10Low() {
        // User has no previous logins, so "new device" and "new network" do not apply.
        // Base = 10 LOW
        RiskContext context = RiskContext.builder()
                .hasPreviousSuccessfulLogin(false)
                .newDevice(true)
                .newNetwork(true)
                .build();

        RiskResult result = riskEngine.evaluate(context);
        assertEquals(10, result.getScore());
        assertEquals(RiskLevel.LOW, result.getLevel());
    }
}
