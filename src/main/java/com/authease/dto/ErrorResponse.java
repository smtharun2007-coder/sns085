package com.authease.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String reasonCode;
    private String title;
    private String message;
    private String nextStep;

    public ErrorResponse() {}

    public ErrorResponse(String reasonCode, String title, String message, String nextStep) {
        this.reasonCode = reasonCode;
        this.title = title;
        this.message = message;
        this.nextStep = nextStep;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNextStep() {
        return nextStep;
    }

    public void setNextStep(String nextStep) {
        this.nextStep = nextStep;
    }
}
