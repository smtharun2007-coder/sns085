package com.authease.dto;

public class TotpSetupResponse {

    private String qrDataUrl;
    private String manualKey;

    public TotpSetupResponse() {}

    public TotpSetupResponse(String qrDataUrl, String manualKey) {
        this.qrDataUrl = qrDataUrl;
        this.manualKey = manualKey;
    }

    public String getQrDataUrl() {
        return qrDataUrl;
    }

    public void setQrDataUrl(String qrDataUrl) {
        this.qrDataUrl = qrDataUrl;
    }

    public String getManualKey() {
        return manualKey;
    }

    public void setManualKey(String manualKey) {
        this.manualKey = manualKey;
    }
}
