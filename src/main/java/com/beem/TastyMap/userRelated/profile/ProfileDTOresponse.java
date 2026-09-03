package com.beem.TastyMap.userRelated.profile;

import com.beem.TastyMap.userRelated.subscribe.RelationStatus;

public class ProfileDTOresponse {
    private String username;
    private String name;
    private String surname;
    private String profile;
    private String role;
    private String biography;
    private long postCount;
    private long subscriberCount;
    private long subscribedCount;
    private boolean blockedByMe;
    private boolean blockedMe;
    private RelationStatus relationStatus;
    private boolean hasPendingIncomingRequest;

    public ProfileDTOresponse() {
    }

    public ProfileDTOresponse(
            String username,
            String name,
            String surname,
            String profile,
            String role,
            String biography,
            long postCount,
            long subscriberCount,
            long subscribedCount,
            boolean blockedByMe,    //ben engelledım
            boolean blockedMe,// o benı engelledı
            RelationStatus relationStatus,
            boolean hasPendingIncomingRequest
    ) {
        this.username = username;
        this.name = name;
        this.surname = surname;
        this.profile = profile;
        this.role = role;
        this.biography = biography;
        this.postCount = postCount;
        this.subscriberCount = subscriberCount;
        this.subscribedCount = subscribedCount;
        this.blockedMe = blockedMe;
        this.blockedByMe = blockedByMe;
        this.relationStatus = relationStatus;
        this.hasPendingIncomingRequest = hasPendingIncomingRequest;
    }

    // Getter ve Setter metotları...
    public boolean isHasPendingIncomingRequest() {
        return hasPendingIncomingRequest;
    }

    public RelationStatus getRelationStatus() {
        return relationStatus;
    }

    public void setRelationStatus(RelationStatus relationStatus) {
        this.relationStatus = relationStatus;
    }

    public void setHasPendingIncomingRequest(boolean hasPendingIncomingRequest) {
        this.hasPendingIncomingRequest = hasPendingIncomingRequest;
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

    public boolean isBlockedByMe() {
        return blockedByMe;
    }

    public void setBlockedByMe(boolean blockedByMe) {
        this.blockedByMe = blockedByMe;
    }

    public boolean isBlockedMe() {
        return blockedMe;
    }

    public void setBlockedMe(boolean blockedMe) {
        this.blockedMe = blockedMe;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }
}
