package com.authease.service;

import com.authease.config.AppProperties;
import com.authease.dto.ExplainResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AssistServiceTest {

    private ReasonCatalogService reasonCatalogService;
    private AppProperties appProperties;
    private AssistService assistService;

    @BeforeEach
    void setUp() {
        reasonCatalogService = new ReasonCatalogService();
        appProperties = new AppProperties();
        assistService = new AssistService(reasonCatalogService, appProperties);
    }

    @Test
    void testExplainFallsBackToTemplateWhenNoAiKey() {
        appProperties.setAiApiKey("");
        ExplainResponse response = assistService.explain("NEW_DEVICE", true);

        assertNotNull(response);
        assertEquals("TEMPLATE", response.getSource());
        assertTrue(response.getText().contains("Sign in from a new device"));
    }

    @Test
    void testExplainTemplateWithoutSimplify() {
        ExplainResponse response = assistService.explain("RATE_DELAY", false);

        assertNotNull(response);
        assertEquals("TEMPLATE", response.getSource());
        assertTrue(response.getText().contains("Too many tries in a short time"));
    }
}
