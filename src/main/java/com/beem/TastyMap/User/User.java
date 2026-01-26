package com.beem.TastyMap.User;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;


    private String profilePhotoUrl;

    private Long createdAt;

    public User() {}

    public User(String username, String profilePhotoUrl) {
        this.username = username;
        this.profilePhotoUrl = profilePhotoUrl;
        this.createdAt = System.currentTimeMillis();
    }
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }


    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }
}
