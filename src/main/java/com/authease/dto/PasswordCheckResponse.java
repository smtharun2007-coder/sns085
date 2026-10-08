package com.authease.dto;

import java.util.List;

public class PasswordCheckResponse {

    private int score; // 0 to 4
    private List<String> hints;
    private boolean breached;

    public PasswordCheckResponse() {}

    public PasswordCheckResponse(int score, List<String> hints, boolean breached) {
        this.score = score;
        this.hints = hints;
        this.breached = breached;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public List<String> getHints() {
        return hints;
    }

    public void setHints(List<String> hints) {
        this.hints = hints;
    }

    public boolean isBreached() {
        return breached;
    }

    public void setBreached(boolean breached) {
        this.breached = breached;
    }
}
