package com.beem.TastyMap.user.account.dto;

import jakarta.validation.constraints.NotBlank;

public class DeleteAccountRequest {
    @NotBlank(message = "{validation.login.password.notblank}")
    private String password;

    private String reason;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
