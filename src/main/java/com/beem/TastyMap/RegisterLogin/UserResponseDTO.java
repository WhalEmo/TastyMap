package com.beem.TastyMap.RegisterLogin;

import java.time.LocalDateTime;

public class UserResponseDTO {
    private Long id;
    private String username;
    private String email;
    private String name;
    private String surname;
    private String profile;
    private String role;
    private LocalDateTime Date;
    private String biography;
    private boolean emailVerified;

    public UserResponseDTO(){}

    public UserResponseDTO(UserEntity user){
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.name = user.getName();
        this.surname = user.getSurname();
        this.profile = user.getProfile();
        this.role=user.getRole();
        Date = user.getDate();
        this.biography = user.getBiography();
        this.emailVerified=user.isEmailVerified();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getDate() {
        return Date;
    }

    public void setDate(LocalDateTime date) {
        Date = date;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}
