package com.beem.TastyMap.search.data;

public class AppUserResult{

    private Long id;
    private String name;
    private String bio;
    private String profile;

    public AppUserResult() {
    }


    public AppUserResult(Long id, String name, String bio, String profile) {
        this.id = id;
        this.name = name;
        this.bio = bio;
        this.profile = profile;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }
}
