package com.authease.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    public static class StepDetail {
        private int current;
        private int total;

        public StepDetail() {}

        public StepDetail(int current, int total) {
            this.current = current;
            this.total = total;
        }

        public int getCurrent() {
            return current;
        }

        public void setCurrent(int current) {
            this.current = current;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }
    }

    private String status; // AUTHENTICATED | MFA_REQUIRED | DELAYED | FAILED
    private String challengeId;
    private String level; // MEDIUM | HIGH
    private List<String> methods; // ["TOTP", "EMAIL_OTP", "BACKUP_CODE"]
    private Boolean needsEmailApproval;
    private StepDetail step;
    private Long retryAfterSeconds;
    private String reasonCode;
    private String title;
    private String message;
    private String nextStep;

    public LoginResponse() {}

    public static LoginResponse authenticated() {
        LoginResponse resp = new LoginResponse();
        resp.setStatus("AUTHENTICATED");
        return resp;
    }

    public static LoginResponse delayed(long retryAfterSeconds, String reasonCode, String title, String message, String nextStep) {
        LoginResponse resp = new LoginResponse();
        resp.setStatus("DELAYED");
        resp.setRetryAfterSeconds(retryAfterSeconds);
        resp.setReasonCode(reasonCode);
        resp.setTitle(title);
        resp.setMessage(message);
        resp.setNextStep(nextStep);
        return resp;
    }

    public static LoginResponse failed(String reasonCode, String title, String message, String nextStep) {
        LoginResponse resp = new LoginResponse();
        resp.setStatus("FAILED");
        resp.setReasonCode(reasonCode);
        resp.setTitle(title);
        resp.setMessage(message);
        resp.setNextStep(nextStep);
        return resp;
    }

    public static LoginResponse mfaRequired(String challengeId, String level, List<String> methods,
                                            boolean needsEmailApproval, int currentStep, int totalSteps,
                                            String reasonCode, String title, String message, String nextStep) {
        LoginResponse resp = new LoginResponse();
        resp.setStatus("MFA_REQUIRED");
        resp.setChallengeId(challengeId);
        resp.setLevel(level);
        resp.setMethods(methods);
        resp.setNeedsEmailApproval(needsEmailApproval);
        resp.setStep(new StepDetail(currentStep, totalSteps));
        resp.setReasonCode(reasonCode);
        resp.setTitle(title);
        resp.setMessage(message);
        resp.setNextStep(nextStep);
        return resp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public List<String> getMethods() {
        return methods;
    }

    public void setMethods(List<String> methods) {
        this.methods = methods;
    }

    public Boolean getNeedsEmailApproval() {
        return needsEmailApproval;
    }

    public void setNeedsEmailApproval(Boolean needsEmailApproval) {
        this.needsEmailApproval = needsEmailApproval;
    }

    public StepDetail getStep() {
        return step;
    }

    public void setStep(StepDetail step) {
        this.step = step;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public void setRetryAfterSeconds(Long retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds;
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
