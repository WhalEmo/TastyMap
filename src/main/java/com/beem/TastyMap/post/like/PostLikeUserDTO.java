package com.beem.TastyMap.post.like;

import com.beem.TastyMap.user.subscribe.model.RelationStatus;
import com.beem.TastyMap.user.subscribe.model.SubscribeStatus;

public class PostLikeUserDTO {
    private Long userId;
    private String username;
    private String profile;
    private RelationStatus relationStatus;

    public PostLikeUserDTO() {
    }

    public PostLikeUserDTO(Long userId, String username, String profile, SubscribeStatus myRequestStatus, SubscribeStatus theirRequestStatus) {
        this.userId = userId;
        this.username = username;
        this.profile = profile;

        if (myRequestStatus == SubscribeStatus.ACCEPTED) {
            this.relationStatus = RelationStatus.FOLLOWING;
        } else if (myRequestStatus == SubscribeStatus.PENDING) {
            this.relationStatus = RelationStatus.PENDING;
        } else if (theirRequestStatus == SubscribeStatus.ACCEPTED) {
            this.relationStatus = RelationStatus.FOLLOW_BACK;
        } else {
            this.relationStatus = RelationStatus.NOT_FOLLOWING;
        }
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getProfile() { return profile; }
    public void setProfile(String profile) { this.profile = profile; }

    public RelationStatus getRelationStatus() { return relationStatus; }
    public void setRelationStatus(RelationStatus relationStatus) { this.relationStatus = relationStatus; }
}