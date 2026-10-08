package com.authease.dto;

import com.authease.model.LoginEvent;

import java.time.Instant;
import java.util.List;

public class AccountSecurityResponse {

    public static class DeviceInfo {
        private String id;
        private String label;
        private Instant lastUsed;
        private boolean current;

        public DeviceInfo() {}

        public DeviceInfo(String id, String label, Instant lastUsed, boolean current) {
            this.id = id;
            this.label = label;
            this.lastUsed = lastUsed;
            this.current = current;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Instant getLastUsed() {
            return lastUsed;
        }

        public void setLastUsed(Instant lastUsed) {
            this.lastUsed = lastUsed;
        }

        public boolean isCurrent() {
            return current;
        }

        public void setCurrent(boolean current) {
            this.current = current;
        }
    }

    private boolean mfaEnabled;
    private List<DeviceInfo> devices;
    private List<LoginEvent> recentEvents;

    public AccountSecurityResponse() {}

    public AccountSecurityResponse(boolean mfaEnabled, List<DeviceInfo> devices, List<LoginEvent> recentEvents) {
        this.mfaEnabled = mfaEnabled;
        this.devices = devices;
        this.recentEvents = recentEvents;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public List<DeviceInfo> getDevices() {
        return devices;
    }

    public void setDevices(List<DeviceInfo> devices) {
        this.devices = devices;
    }

    public List<LoginEvent> getRecentEvents() {
        return recentEvents;
    }

    public void setRecentEvents(List<LoginEvent> recentEvents) {
        this.recentEvents = recentEvents;
    }
}
