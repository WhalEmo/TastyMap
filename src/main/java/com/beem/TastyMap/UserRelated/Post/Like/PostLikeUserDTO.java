package com.beem.TastyMap.UserRelated.Post.Like;

public class PostLikeUserDTO{
    private Long userId;
    private String username;
    private String profile;
    private boolean isFollow;
    private boolean followsMe;
    private boolean mutual;


    public PostLikeUserDTO() {
    }

    public PostLikeUserDTO(Long userId, String username, String profile, boolean isFollow, boolean followsMe, boolean mutual) {
        this.userId = userId;
        this.username = username;
        this.profile = profile;
        this.isFollow = isFollow;
        this.followsMe = followsMe;
        this.mutual = mutual;
    }

    public boolean isFollowsMe() {
        return followsMe;
    }

    public void setFollowsMe(boolean followsMe) {
        this.followsMe = followsMe;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public boolean isiFollow() {
        return isFollow;
    }

    public void setiFollow(boolean iFollow) {
        this.isFollow = iFollow;
    }

    public boolean isMutual() {
        return mutual;
    }

    public void setMutual(boolean mutual) {
        this.mutual = mutual;
    }
}
