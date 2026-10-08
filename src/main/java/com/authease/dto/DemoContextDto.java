package com.authease.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DemoContextDto {

    private Boolean newDevice;
    private Boolean newNetwork;
    private Boolean unusualHour;
    private Integer hourOfDay;
    private Integer recentFailures;
    private Object forceFailures; // can be Boolean or Integer
    private Boolean highRiskFlag;

    public DemoContextDto() {}

    public DemoContextDto(Boolean newDevice, Boolean newNetwork, Boolean unusualHour,
                          Integer hourOfDay, Integer recentFailures, Object forceFailures,
                          Boolean highRiskFlag) {
        this.newDevice = newDevice;
        this.newNetwork = newNetwork;
        this.unusualHour = unusualHour;
        this.hourOfDay = hourOfDay;
        this.recentFailures = recentFailures;
        this.forceFailures = forceFailures;
        this.highRiskFlag = highRiskFlag;
    }

    public Boolean getNewDevice() {
        return newDevice;
    }

    public void setNewDevice(Boolean newDevice) {
        this.newDevice = newDevice;
    }

    public Boolean getNewNetwork() {
        return newNetwork;
    }

    public void setNewNetwork(Boolean newNetwork) {
        this.newNetwork = newNetwork;
    }

    public Boolean getUnusualHour() {
        return unusualHour;
    }

    public void setUnusualHour(Boolean unusualHour) {
        this.unusualHour = unusualHour;
    }

    public Integer getHourOfDay() {
        return hourOfDay;
    }

    public void setHourOfDay(Integer hourOfDay) {
        this.hourOfDay = hourOfDay;
    }

    public Integer getRecentFailures() {
        return recentFailures;
    }

    public void setRecentFailures(Integer recentFailures) {
        this.recentFailures = recentFailures;
    }

    public Object getForceFailures() {
        return forceFailures;
    }

    public void setForceFailures(Object forceFailures) {
        this.forceFailures = forceFailures;
    }

    public Boolean getHighRiskFlag() {
        return highRiskFlag;
    }

    public void setHighRiskFlag(Boolean highRiskFlag) {
        this.highRiskFlag = highRiskFlag;
    }

    public int resolveEffectiveFailures() {
        if (recentFailures != null && recentFailures > 0) {
            return recentFailures;
        }
        if (forceFailures instanceof Boolean b && Boolean.TRUE.equals(b)) {
            return 3;
        }
        if (forceFailures instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }
}
