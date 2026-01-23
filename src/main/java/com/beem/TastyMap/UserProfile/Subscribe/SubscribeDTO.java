package com.beem.TastyMap.UserProfile.Subscribe;

public class SubscribeDTO {
    private Long id;
    private String profile;
    private String username;

    public SubscribeDTO() {
    }

    public SubscribeDTO(Long id, String profile, String username) {
        this.id = id;
        this.profile = profile;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
