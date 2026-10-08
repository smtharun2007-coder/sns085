package com.authease.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "trusted_devices")
public class TrustedDevice {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String tokenHash;

    private String label;
    private Instant createdAt = Instant.now();
    private Instant lastUsedAt = Instant.now();

    @Indexed(expireAfter = "0s")
    private Instant expiresAt;

    public TrustedDevice() {}

    public TrustedDevice(String userId, String tokenHash, String label, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.label = label;
        this.createdAt = Instant.now();
        this.lastUsedAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
