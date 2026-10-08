package com.authease.dto;

import jakarta.validation.constraints.NotBlank;

public class ExplainRequest {

    @NotBlank(message = "Reason code is required")
    private String reasonCode;

    private boolean simplify = false;

    public ExplainRequest() {}

    public ExplainRequest(String reasonCode, boolean simplify) {
        this.reasonCode = reasonCode;
        this.simplify = simplify;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public boolean isSimplify() {
        return simplify;
    }

    public void setSimplify(boolean simplify) {
        this.simplify = simplify;
    }
}
