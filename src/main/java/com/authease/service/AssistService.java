package com.authease.service;

import com.authease.config.AppProperties;
import com.authease.dto.ExplainResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class AssistService {

    private static final Logger log = LoggerFactory.getLogger(AssistService.class);

    private final ReasonCatalogService reasonCatalogService;
    private final AppProperties appProperties;
    private final HttpClient httpClient;

    public AssistService(ReasonCatalogService reasonCatalogService, AppProperties appProperties) {
        this.reasonCatalogService = reasonCatalogService;
        this.appProperties = appProperties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    public ExplainResponse explain(String reasonCode, boolean simplify) {
        ReasonCatalogService.ReasonDetail detail = reasonCatalogService.get(reasonCode);
        String templateText = detail.title() + ". " + detail.message() + " " + detail.nextStep();

        if (!simplify || appProperties.getAiApiKey() == null || appProperties.getAiApiKey().isBlank()) {
            return new ExplainResponse(templateText, "TEMPLATE");
        }

        try {
            // Rephrase template text with 3-second timeout.
            // Strict safety: Only template text is sent. No emails, IPs, passwords, or tokens.
            String rephrased = callLlmForSimplification(templateText);
            if (rephrased != null && !rephrased.isBlank()) {
                return new ExplainResponse(rephrased.trim(), "LLM");
            }
        } catch (Exception e) {
            log.warn("AI assistance call failed or timed out ({}), falling back to template text.", e.getMessage());
        }

        return new ExplainResponse(templateText, "TEMPLATE");
    }

    private String callLlmForSimplification(String textToSimplify) {
        try {
            // Fast call to LLM with 3-second timeout constraint
            String apiKey = appProperties.getAiApiKey();
            String prompt = "Rewrite this security notice in very simple English suitable for an elementary reader. Return only the rewritten text: " + textToSimplify;
            String jsonPayload = String.format("{\"contents\": [{\"parts\": [{\"text\": %s}]}]}",
                    escapeJson(prompt));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(3))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                // Parse simple text or fallback
                String body = response.body();
                int textIdx = body.indexOf("\"text\": \"");
                if (textIdx != -1) {
                    int start = textIdx + 9;
                    int end = body.indexOf("\"", start);
                    if (end != -1) {
                        return body.substring(start, end).replace("\\n", "\n").replace("\\\"", "\"");
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String escapeJson(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
