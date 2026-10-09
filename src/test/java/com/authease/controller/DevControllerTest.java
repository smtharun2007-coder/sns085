package com.authease.controller;

import com.authease.model.EmailOutbox;
import com.authease.repository.EmailOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DevControllerTest {

    private EmailOutboxRepository outboxRepository;
    private DevController devController;

    @BeforeEach
    void setUp() {
        outboxRepository = Mockito.mock(EmailOutboxRepository.class);
        devController = new DevController(outboxRepository);
    }

    @Test
    void testGetOutboxReturnsAllEmailsGlobally() {
        List<EmailOutbox> list = List.of(
                new EmailOutbox("s1", "user1@example.com", "Verify 1", "Body 1"),
                new EmailOutbox("s2", "user2@example.com", "Verify 2", "Body 2")
        );
        when(outboxRepository.findAll(any(Sort.class))).thenReturn(list);

        ResponseEntity<List<EmailOutbox>> response = devController.getOutbox(null);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(2, response.getBody().size());
        assertEquals("user1@example.com", response.getBody().get(0).getTo());
    }

    @Test
    void testGetOutboxFilteredByRecipient() {
        List<EmailOutbox> list = List.of(
                new EmailOutbox("s1", "target@example.com", "Target Email", "Body")
        );
        when(outboxRepository.findByToIgnoreCaseOrderByTsDesc(eq("target@example.com"))).thenReturn(list);

        ResponseEntity<List<EmailOutbox>> response = devController.getOutbox("target@example.com");
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("target@example.com", response.getBody().get(0).getTo());
    }

    @Test
    void testGetOutboxCappedAt50() {
        List<EmailOutbox> many = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            EmailOutbox msg = new EmailOutbox("s", "u" + i + "@example.com", "Subj " + i, "Body " + i);
            msg.setTs(Instant.now());
            many.add(msg);
        }
        when(outboxRepository.findAll(any(Sort.class))).thenReturn(many);

        ResponseEntity<List<EmailOutbox>> response = devController.getOutbox(null);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(50, response.getBody().size());
    }
}
