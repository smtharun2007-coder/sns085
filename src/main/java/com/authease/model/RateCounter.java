package com.authease.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "rate_counters")
public class RateCounter {

    @Id
    private String id; // keyHash: sha256(ip + ":" + identifier)

    private int failures = 0;
    private Instant nextAllowedAt;

    @Indexed(expireAfter = "0s")
    private Instant expiresAt;

    public RateCounter() {}

    public RateCounter(String id, int failures, Instant nextAllowedAt, Instant expiresAt) {
        this.id = id;
        this.failures = failures;
        this.nextAllowedAt = nextAllowedAt;
        this.expiresAt = expiresAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getFailures() {
        return failures;
    }

    public void setFailures(int failures) {
        this.failures = failures;
    }

    public Instant getNextAllowedAt() {
        return nextAllowedAt;
    }

    public void setNextAllowedAt(Instant nextAllowedAt) {
        this.nextAllowedAt = nextAllowedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
