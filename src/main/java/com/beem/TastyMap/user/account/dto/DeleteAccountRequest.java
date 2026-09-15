package com.beem.TastyMap.user.account.dto;

import com.beem.TastyMap.user.account.model.DeleteReason;
import jakarta.validation.constraints.NotBlank;

public class DeleteAccountRequest {
    @NotBlank(message = "{validation.login.password.notblank}")
    private String password;

    private String customReason;

    private DeleteReason reasonType;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCustomReason() {
        return customReason;
    }

    public void setCustomReason(String customReason) {
        this.customReason = customReason;
    }

    public DeleteReason getReasonType() {
        return reasonType;
    }

    public void setReasonType(DeleteReason reasonType) {
        this.reasonType = reasonType;
    }
}
