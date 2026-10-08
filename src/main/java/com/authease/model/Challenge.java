package com.authease.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "challenges")
public class Challenge {

    @Id
    private String id; // random 256-bit hex

    @Indexed
    private String userId;

    @Indexed
    private String boundSessionId;

    private RiskLevel level;
    private int requiredSteps;
    private int completedSteps = 1; // Step 1 is password
    private Boolean emailApproved = false;
    private String approvalTokenHash;
    private int wrongCodeCount = 0;
    private Instant minReleaseAt;

    private String emailOtpHash;
    private Instant emailOtpExpiresAt;
    private Long lastUsedTotpTimeStep;

    @Indexed(expireAfter = "0s")
    private Instant expiresAt;

    public Challenge() {}

    public Challenge(String id, String userId, String boundSessionId, RiskLevel level,
                     int requiredSteps, Instant minReleaseAt, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.boundSessionId = boundSessionId;
        this.level = level;
        this.requiredSteps = requiredSteps;
        this.completedSteps = 1;
        this.emailApproved = false;
        this.wrongCodeCount = 0;
        this.minReleaseAt = minReleaseAt;
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

    public String getBoundSessionId() {
        return boundSessionId;
    }

    public void setBoundSessionId(String boundSessionId) {
        this.boundSessionId = boundSessionId;
    }

    public RiskLevel getLevel() {
        return level;
    }

    public void setLevel(RiskLevel level) {
        this.level = level;
    }

    public int getRequiredSteps() {
        return requiredSteps;
    }

    public void setRequiredSteps(int requiredSteps) {
        this.requiredSteps = requiredSteps;
    }

    public int getCompletedSteps() {
        return completedSteps;
    }

    public void setCompletedSteps(int completedSteps) {
        this.completedSteps = completedSteps;
    }

    public Boolean getEmailApproved() {
        return emailApproved;
    }

    public void setEmailApproved(Boolean emailApproved) {
        this.emailApproved = emailApproved;
    }

    public String getApprovalTokenHash() {
        return approvalTokenHash;
    }

    public void setApprovalTokenHash(String approvalTokenHash) {
        this.approvalTokenHash = approvalTokenHash;
    }

    public int getWrongCodeCount() {
        return wrongCodeCount;
    }

    public void setWrongCodeCount(int wrongCodeCount) {
        this.wrongCodeCount = wrongCodeCount;
    }

    public Instant getMinReleaseAt() {
        return minReleaseAt;
    }

    public void setMinReleaseAt(Instant minReleaseAt) {
        this.minReleaseAt = minReleaseAt;
    }

    public String getEmailOtpHash() {
        return emailOtpHash;
    }

    public void setEmailOtpHash(String emailOtpHash) {
        this.emailOtpHash = emailOtpHash;
    }

    public Instant getEmailOtpExpiresAt() {
        return emailOtpExpiresAt;
    }

    public void setEmailOtpExpiresAt(Instant emailOtpExpiresAt) {
        this.emailOtpExpiresAt = emailOtpExpiresAt;
    }

    public Long getLastUsedTotpTimeStep() {
        return lastUsedTotpTimeStep;
    }

    public void setLastUsedTotpTimeStep(Long lastUsedTotpTimeStep) {
        this.lastUsedTotpTimeStep = lastUsedTotpTimeStep;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
