package com.authease.dto;

import java.util.List;

public class LastDecisionResponse {

    public static class SignalDetail {
        private String code;
        private String label;
        private int points;

        public SignalDetail() {}

        public SignalDetail(String code, String label, int points) {
            this.code = code;
            this.label = label;
            this.points = points;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public int getPoints() {
            return points;
        }

        public void setPoints(int points) {
            this.points = points;
        }
    }

    private Integer score;
    private String level;
    private List<SignalDetail> signals;

    public LastDecisionResponse() {}

    public LastDecisionResponse(Integer score, String level, List<SignalDetail> signals) {
        this.score = score;
        this.level = level;
        this.signals = signals;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public List<SignalDetail> getSignals() {
        return signals;
    }

    public void setSignals(List<SignalDetail> signals) {
        this.signals = signals;
    }
}
