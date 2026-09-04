package com.beem.TastyMap.userRelated.subscribe;

public class SubscribeDTO {
    private Long id;
    private String profile;
    private String username;
    private RelationStatus relationStatus; // FOLLOWING, FOLLOW_BACK, PENDING, NOT_FOLLOWING, SELF

    public SubscribeDTO() {
    }

    public SubscribeDTO(Long id, String profile, String username, RelationStatus relationStatus) {
        this.id = id;
        this.profile = profile;
        this.username = username;
        this.relationStatus = relationStatus;
    }
    public SubscribeDTO(Long id, String profile, String username, String relationStatusStr) {
        this.id = id;
        this.profile = profile;
        this.username = username;
        this.relationStatus = relationStatusStr != null ? RelationStatus.valueOf(relationStatusStr) : RelationStatus.NOT_FOLLOWING;
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

    public RelationStatus getRelationStatus() {
        return relationStatus;
    }

    public void setRelationStatus(RelationStatus relationStatus) {
        this.relationStatus = relationStatus;
    }
}