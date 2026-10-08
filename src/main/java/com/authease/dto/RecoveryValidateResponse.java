package com.authease.dto;

public class RecoveryValidateResponse {

    private boolean valid;
    private boolean requiresSecondFactor;

    public RecoveryValidateResponse() {}

    public RecoveryValidateResponse(boolean valid, boolean requiresSecondFactor) {
        this.valid = valid;
        this.requiresSecondFactor = requiresSecondFactor;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean isRequiresSecondFactor() {
        return requiresSecondFactor;
    }

    public void setRequiresSecondFactor(boolean requiresSecondFactor) {
        this.requiresSecondFactor = requiresSecondFactor;
    }
}
