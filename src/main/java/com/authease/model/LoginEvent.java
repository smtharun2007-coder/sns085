package com.authease.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "login_events")
public class LoginEvent {

    @Id
    private String id;

    @Indexed
    private Instant ts = Instant.now();

    @Indexed
    private String userIdOrNull;

    @Indexed
    private String identifierHash;

    private LoginEventType type;
    private RiskLevel level;
    private Integer score;
    private List<String> signals = new ArrayList<>();
    private String reasonCode;
    private String ipPrefixHash;
    private String deviceId;

    public LoginEvent() {}

    public LoginEvent(Instant ts, String userIdOrNull, String identifierHash, LoginEventType type,
                      RiskLevel level, Integer score, List<String> signals, String reasonCode,
                      String ipPrefixHash, String deviceId) {
        this.ts = ts != null ? ts : Instant.now();
        this.userIdOrNull = userIdOrNull;
        this.identifierHash = identifierHash;
        this.type = type;
        this.level = level;
        this.score = score;
        this.signals = signals != null ? signals : new ArrayList<>();
        this.reasonCode = reasonCode;
        this.ipPrefixHash = ipPrefixHash;
        this.deviceId = deviceId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Instant getTs() {
        return ts;
    }

    public void setTs(Instant ts) {
        this.ts = ts;
    }

    public String getUserIdOrNull() {
        return userIdOrNull;
    }

    public void setUserIdOrNull(String userIdOrNull) {
        this.userIdOrNull = userIdOrNull;
    }

    public String getIdentifierHash() {
        return identifierHash;
    }

    public void setIdentifierHash(String identifierHash) {
        this.identifierHash = identifierHash;
    }

    public LoginEventType getType() {
        return type;
    }

    public void setType(LoginEventType type) {
        this.type = type;
    }

    public RiskLevel getLevel() {
        return level;
    }

    public void setLevel(RiskLevel level) {
        this.level = level;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public List<String> getSignals() {
        return signals;
    }

    public void setSignals(List<String> signals) {
        this.signals = signals;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getIpPrefixHash() {
        return ipPrefixHash;
    }

    public void setIpPrefixHash(String ipPrefixHash) {
        this.ipPrefixHash = ipPrefixHash;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
