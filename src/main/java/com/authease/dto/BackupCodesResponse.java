package com.authease.dto;

import java.util.List;

public class BackupCodesResponse {

    private List<String> backupCodes;

    public BackupCodesResponse() {}

    public BackupCodesResponse(List<String> backupCodes) {
        this.backupCodes = backupCodes;
    }

    public List<String> getBackupCodes() {
        return backupCodes;
    }

    public void setBackupCodes(List<String> backupCodes) {
        this.backupCodes = backupCodes;
    }
}
