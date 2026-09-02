package com.beem.TastyMap.userRelated.profile;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateProfileDTO {

    @Size(min = 3, max = 20, message = "Kullanıcı adı 3–20 karakter arasında olmalıdır")
    @Pattern(
            regexp = "^[a-zA-Z0-9._]+$",
            message = "Kullanıcı adı sadece harf, rakam, nokta ve alt çizgi içerebilir"
    )
    private String username;

    @Size(min = 2, max = 50, message = "Ad 2–50 karakter arasında olmalıdır")
    @Pattern(
            regexp = "^[a-zA-ZçÇğĞıİöÖşŞüÜ ]+$",
            message = "Ad sadece harf ve boşluk içerebilir"
    )
    private String name;

    @Size(min = 2, max = 50, message = "Soyad 2–50 karakter arasında olmalıdır")
    @Pattern(
            regexp = "^[a-zA-ZçÇğĞıİöÖşŞüÜ ]+$",
            message = "Soyad sadece harf ve boşluk içerebilir"
    )
    private String surname;

    private String profilePhoto;

    @Size(max = 200, message = "Biyografi en fazla 200 karakter olabilir")
    private String biography;


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

    public String getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(String profilePhoto) {
        this.profilePhoto = profilePhoto;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }
}