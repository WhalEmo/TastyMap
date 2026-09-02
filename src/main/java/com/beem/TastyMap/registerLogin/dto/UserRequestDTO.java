package com.beem.TastyMap.registerLogin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserRequestDTO {
    @NotBlank(message = "{validation.user.username.notblank}")
    @Size(min = 3, max = 20, message = "{validation.user.username.size}")
    @Pattern(
            regexp = "^[a-zA-Z0-9._]+$",
            message = "{validation.user.username.pattern}"
    )
    private String username;

    @NotBlank(message = "{validation.user.name.notblank}")
    @Size(min = 2, max = 50, message = "{validation.user.name.size}")
    @Pattern(
            regexp = "^[a-zA-ZçÇğĞıİöÖşŞüÜ ]+$",
            message = "{validation.user.name.pattern}"
    )
    private String name;

    @NotBlank(message = "{validation.user.surname.notblank}")
    @Size(min = 2, max = 50, message = "{validation.user.surname.size}")
    @Pattern(
            regexp = "^[a-zA-ZçÇğĞıİöÖşŞüÜ]+$",
            message = "{validation.user.surname.pattern}"
    )
    private String surname;

    @Email(message = "{validation.user.email.invalid}")
    @NotBlank(message = "{validation.user.email.notblank}")
    private String email;

    @NotBlank(message = "{validation.user.password.notblank}")
    @Size(min = 6, message = "{validation.user.password.size}")
    private String password;

    private String profile;

    @Size(max = 200, message = "{validation.user.biography.size}")
    private String biography;

    private String role;
    private boolean privateProfile;

    private String deviceId;

    public boolean isPrivateProfile() {
        return privateProfile;
    }

    public void setPrivateProfile(boolean privateProfile) {
        this.privateProfile = privateProfile;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}