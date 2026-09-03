package com.beem.TastyMap.userRelated.subscribe;

public class SubscribeDTO {
    private Long id;
    private String profile;
    private String username;
    private SubscribeStatus relationStatus; // SELF, ACCEPTED, PENDING veya null (FOLLOW_NONE)

    public SubscribeDTO() {
    }

    public SubscribeDTO(Long id, String profile, String username, SubscribeStatus relationStatus) {
        this.id = id;
        this.profile = profile;
        this.username = username;
        this.relationStatus = relationStatus;
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

    public SubscribeStatus getRelationStatus() {
        return relationStatus;
    }

    public void setRelationStatus(SubscribeStatus relationStatus) {
        this.relationStatus = relationStatus;
    }
}