package com.authease.risk;

import com.authease.model.RiskLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RiskResult {

    private final int score;
    private final RiskLevel level;
    private final List<RiskSignal> signals;

    public RiskResult(int score, RiskLevel level, List<RiskSignal> signals) {
        this.score = score;
        this.level = level;
        this.signals = signals != null ? Collections.unmodifiableList(signals) : Collections.emptyList();
    }

    public int getScore() {
        return score;
    }

    public RiskLevel getLevel() {
        return level;
    }

    public List<RiskSignal> getSignals() {
        return signals;
    }
}
