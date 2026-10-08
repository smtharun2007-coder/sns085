package com.authease.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthMeResponse {

    public static class UserSummary {
        private String email;
        private String displayName;
        private Set<String> roles;
        private boolean mfaEnabled;

        public UserSummary() {}

        public UserSummary(String email, String displayName, Set<String> roles, boolean mfaEnabled) {
            this.email = email;
            this.displayName = displayName;
            this.roles = roles;
            this.mfaEnabled = mfaEnabled;
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

        public Set<String> getRoles() {
            return roles;
        }

        public void setRoles(Set<String> roles) {
            this.roles = roles;
        }

        public boolean isMfaEnabled() {
            return mfaEnabled;
        }

        public void setMfaEnabled(boolean mfaEnabled) {
            this.mfaEnabled = mfaEnabled;
        }
    }

    private boolean authenticated;
    private UserSummary user;
    private Long sessionExpiresInSeconds;

    public AuthMeResponse() {}

    public static AuthMeResponse unauthenticated() {
        AuthMeResponse resp = new AuthMeResponse();
        resp.setAuthenticated(false);
        return resp;
    }

    public static AuthMeResponse authenticated(UserSummary user, Long sessionExpiresInSeconds) {
        AuthMeResponse resp = new AuthMeResponse();
        resp.setAuthenticated(true);
        resp.setUser(user);
        resp.setSessionExpiresInSeconds(sessionExpiresInSeconds);
        return resp;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public UserSummary getUser() {
        return user;
    }

    public void setUser(UserSummary user) {
        this.user = user;
    }

    public Long getSessionExpiresInSeconds() {
        return sessionExpiresInSeconds;
    }

    public void setSessionExpiresInSeconds(Long sessionExpiresInSeconds) {
        this.sessionExpiresInSeconds = sessionExpiresInSeconds;
    }
}
