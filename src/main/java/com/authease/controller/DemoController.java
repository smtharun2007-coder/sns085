package com.authease.controller;

import com.authease.dto.DemoContextDto;
import com.authease.service.DemoContextService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final DemoContextService demoContextService;

    public DemoController(DemoContextService demoContextService) {
        this.demoContextService = demoContextService;
    }

    @GetMapping("/context")
    public ResponseEntity<DemoContextDto> getDemoContext(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        DemoContextDto context = demoContextService.getContext(session.getId());
        return ResponseEntity.ok(context);
    }

    @PostMapping("/context")
    public ResponseEntity<DemoContextDto> setDemoContext(@RequestBody(required = false) DemoContextDto dto,
                                                        HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        DemoContextDto updated = demoContextService.setContext(session.getId(), dto);
        return ResponseEntity.ok(updated);
    }
}
