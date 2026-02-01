package com.beem.TastyMap.RegisterLogin;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class UserRequestDTO {
    private Long id;
    @NotBlank(message = "Kullanıcı adı boş olamaz")
    @Size(min = 3, max = 20, message = "Kullanıcı adı 3–20 karakter arasında olmalıdır")
    @Pattern(
            regexp = "^[a-zA-Z0-9._]+$",
            message = "Kullanıcı adı sadece harf, rakam, nokta ve alt çizgi içerebilir"
    )
    private String username;

    @NotBlank(message = "Ad boş olamaz")
    @Size(min = 2, max = 20, message = "Ad 2–50 karakter arasında olmalıdır")
    @Pattern(
            regexp = "^[a-zA-ZçÇğĞıİöÖşŞüÜ ]+$",
            message = "Ad sadece harf ve boşluk içerebilir"
    )
    private String name;

    @NotBlank(message = "Soyad boş olamaz")
    @Size(min = 2, max = 20, message = "Soyad 2–50 karakter arasında olmalıdır")
    @Pattern(
            regexp = "^[a-zA-ZçÇğĞıİöÖşŞüÜ ]+$",
            message = "Soyad sadece harf ve boşluk içerebilir"
    )
    private String surname;

    @Email(message = "Geçerli bir email girin")
    @NotBlank(message = "Email boş olamaz")
    private String email;

    @NotBlank(message = "Parola boş olamaz")
    @Size(min = 6, message = "Parola en az 6 karakter olmalı")
    private String password;

    private String profile;

    @Size(max = 200, message = "Biyografi en fazla 200 karakter olabilir")
    private String biography;

    private String role;
    private boolean privateProfile;

    public Long getId() {
        return id;
    }

    public boolean isPrivateProfile() {
        return privateProfile;
    }

    public void setPrivateProfile(boolean privateProfile) {
        this.privateProfile = privateProfile;
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
}
