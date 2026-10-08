package com.authease.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String displayName;
    private String passwordHash;
    private boolean emailVerified = false;
    private Set<String> roles = new HashSet<>();
    private String totpSecretEncrypted;
    private boolean totpEnabled = false;
    private List<String> backupCodeHashes = new ArrayList<>();
    private List<String> knownNetworkHashes = new ArrayList<>();
    private List<Integer> loginHours = new ArrayList<>();
    private Instant lastSuccessfulLoginAt;
    private Instant createdAt = Instant.now();

    public User() {}

    public User(String email, String displayName, String passwordHash) {
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.emailVerified = false;
        this.roles.add("USER");
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public String getTotpSecretEncrypted() {
        return totpSecretEncrypted;
    }

    public void setTotpSecretEncrypted(String totpSecretEncrypted) {
        this.totpSecretEncrypted = totpSecretEncrypted;
    }

    public boolean isTotpEnabled() {
        return totpEnabled;
    }

    public void setTotpEnabled(boolean totpEnabled) {
        this.totpEnabled = totpEnabled;
    }

    public List<String> getBackupCodeHashes() {
        return backupCodeHashes;
    }

    public void setBackupCodeHashes(List<String> backupCodeHashes) {
        this.backupCodeHashes = backupCodeHashes;
    }

    public List<String> getKnownNetworkHashes() {
        return knownNetworkHashes;
    }

    public void setKnownNetworkHashes(List<String> knownNetworkHashes) {
        this.knownNetworkHashes = knownNetworkHashes;
    }

    public List<Integer> getLoginHours() {
        return loginHours;
    }

    public void setLoginHours(List<Integer> loginHours) {
        this.loginHours = loginHours;
    }

    public Instant getLastSuccessfulLoginAt() {
        return lastSuccessfulLoginAt;
    }

    public void setLastSuccessfulLoginAt(Instant lastSuccessfulLoginAt) {
        this.lastSuccessfulLoginAt = lastSuccessfulLoginAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
