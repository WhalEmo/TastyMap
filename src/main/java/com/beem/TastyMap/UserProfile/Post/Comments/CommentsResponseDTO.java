package com.beem.TastyMap.UserProfile.Post.Comments;

import java.time.LocalDateTime;

public class CommentsResponseDTO {
    private Long id;
    private Long parentYorumId;
    private String contents;
    private LocalDateTime date;

    private Long userId;
    private String username;
    private String profilePhotoUrl;

    public CommentsResponseDTO() {
    }

    public CommentsResponseDTO(Long id, Long parentYorumId, String contents, LocalDateTime date, Long userId, String username, String profilePhotoUrl) {
        this.id = id;
        this.parentYorumId = parentYorumId;
        this.contents = contents;
        this.date = date;
        this.userId = userId;
        this.username = username;
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentYorumId() {
        return parentYorumId;
    }

    public void setParentYorumId(Long parentYorumId) {
        this.parentYorumId = parentYorumId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }
}
