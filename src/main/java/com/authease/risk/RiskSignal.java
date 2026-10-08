package com.authease.risk;

public class RiskSignal {

    private final String code;
    private final String label;
    private final int points;

    public RiskSignal(String code, String label, int points) {
        this.code = code;
        this.label = label;
        this.points = points;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public int getPoints() {
        return points;
    }
}
