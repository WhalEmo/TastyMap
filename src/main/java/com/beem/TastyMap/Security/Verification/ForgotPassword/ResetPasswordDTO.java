package com.beem.TastyMap.Security.Verification.ForgotPassword;

public class ResetPasswordDTO {
    private String newPassword;

    public ResetPasswordDTO(){

    }
    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
