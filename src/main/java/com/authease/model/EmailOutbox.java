package com.authease.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "email_outbox")
public class EmailOutbox {

    @Id
    private String id;

    private String demoSessionId;
    private String to;
    private String subject;
    private String body;
    private Instant ts = Instant.now();

    public EmailOutbox() {}

    public EmailOutbox(String demoSessionId, String to, String subject, String body) {
        this.demoSessionId = demoSessionId;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.ts = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDemoSessionId() {
        return demoSessionId;
    }

    public void setDemoSessionId(String demoSessionId) {
        this.demoSessionId = demoSessionId;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Instant getTs() {
        return ts;
    }

    public void setTs(Instant ts) {
        this.ts = ts;
    }
}
