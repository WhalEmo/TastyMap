package com.beem.TastyMap.UserProfile.Subscribe;

public class ProfileDTOresponse {
    private String username;
    private String name;
    private String profile;
    private String role;
    private String biography;

    private long postCount;
    private long followerCount;
    private long followingCount;

    public ProfileDTOresponse() {
    }

    public ProfileDTOresponse(
            String username,
            String name,
            String profile,
            String role,
            String biography,
            long postCount,
            long followerCount,
            long followingCount
    ) {
        this.username = username;
        this.name = name;
        this.profile = profile;
        this.role = role;
        this.biography = biography;
        this.postCount = postCount;
        this.followerCount = followerCount;
        this.followingCount = followingCount;
    }

    // getters & setters

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

    public long getFollowerCount() {
        return followerCount;
    }

    public void setFollowerCount(long followerCount) {
        this.followerCount = followerCount;
    }

    public long getFollowingCount() {
        return followingCount;
    }

    public void setFollowingCount(long followingCount) {
        this.followingCount = followingCount;
    }
}
