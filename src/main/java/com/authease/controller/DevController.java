package com.authease.controller;

import com.authease.model.EmailOutbox;
import com.authease.repository.EmailOutboxRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dev")
public class DevController {

    private final EmailOutboxRepository outboxRepository;

    public DevController(EmailOutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @GetMapping("/outbox")
    public ResponseEntity<List<EmailOutbox>> getOutbox(HttpServletRequest request) {
        String sessionId = resolveDemoSessionId(request);
        List<EmailOutbox> outboxList = outboxRepository.findByDemoSessionIdOrderByTsDesc(sessionId);
        if (outboxList.isEmpty()) {
            List<EmailOutbox> all = outboxRepository.findAll(Sort.by(Sort.Direction.DESC, "ts"));
            if (all.size() > 25) {
                all = all.subList(0, 25);
            }
            return ResponseEntity.ok(all);
        }
        return ResponseEntity.ok(outboxList);
    }

    private String resolveDemoSessionId(HttpServletRequest request) {
        String headerSession = request.getHeader("X-Demo-Session");
        if (headerSession != null && !headerSession.isBlank()) {
            return headerSession.trim();
        }
        HttpSession session = request.getSession(false);
        return session != null ? session.getId() : "default";
    }
}
