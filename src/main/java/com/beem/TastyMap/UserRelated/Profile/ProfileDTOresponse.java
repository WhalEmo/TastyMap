package com.beem.TastyMap.UserRelated.Profile;

public class ProfileDTOresponse {
    private String username;
    private String name;
    private String profile;
    private String role;
    private String biography;

    private long postCount;
    private long subscriberCount;
    private long subscribedCount;

    public ProfileDTOresponse() {
    }

    public ProfileDTOresponse(
            String username,
            String name,
            String profile,
            String role,
            String biography,
            long postCount,
            long subscriberCount,
            long subscribedCount
    ) {
        this.username = username;
        this.name = name;
        this.profile = profile;
        this.role = role;
        this.biography = biography;
        this.postCount = postCount;
        this.subscriberCount = subscriberCount;
        this.subscribedCount = subscribedCount;
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

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public long getPostCount() {
        return postCount;
    }

    public void setPostCount(long postCount) {
        this.postCount = postCount;
    }

    public long getSubscriberCount() {
        return subscriberCount;
    }

    public void setSubscriberCount(long subscriberCount) {
        this.subscriberCount = subscriberCount;
    }

    public long getSubscribedCount() {
        return subscribedCount;
    }

    public void setSubscribedCount(long subscribedCount) {
        this.subscribedCount = subscribedCount;
    }
}
