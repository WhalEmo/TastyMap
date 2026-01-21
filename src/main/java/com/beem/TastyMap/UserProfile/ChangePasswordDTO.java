package com.beem.TastyMap.UserProfile;

import jakarta.validation.constraints.NotBlank;

public class ChangePasswordDTO {
    @NotBlank(message = "Eski Şifre boş olamaz!")
    private String oldPassword;

    @NotBlank(message = "Yeni Şifre boş olamaz!")
    private String newPassword;

    @NotBlank(message = "Yeni Şifre boş olamaz!")
    private String againNew;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getAgainNew() {
        return againNew;
    }

    public void setAgainNew(String againNew) {
        this.againNew = againNew;
    }
}
