package com.authease.controller;

import com.authease.dto.ExplainRequest;
import com.authease.dto.ExplainResponse;
import com.authease.service.AssistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assist")
public class AssistController {

    private final AssistService assistService;

    public AssistController(AssistService assistService) {
        this.assistService = assistService;
    }

    @PostMapping("/explain")
    public ResponseEntity<ExplainResponse> explain(@RequestBody ExplainRequest request) {
        String reasonCode = (request != null && request.getReasonCode() != null)
                ? request.getReasonCode()
                : "DEFAULT";
        boolean simplify = request != null && request.isSimplify();

        ExplainResponse response = assistService.explain(reasonCode, simplify);
        return ResponseEntity.ok(response);
    }
}
