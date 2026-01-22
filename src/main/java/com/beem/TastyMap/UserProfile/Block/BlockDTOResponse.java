package com.beem.TastyMap.UserProfile.Block;

import java.time.LocalDateTime;

public class BlockDTOResponse {
    private Long userId;
    private String username;
    private String profilephoto;
    private LocalDateTime blockedAt;

    public BlockDTOResponse() {
    }
    public BlockDTOResponse(Long userId, String username, String profilephoto, LocalDateTime blockedAt) {
        this.userId = userId;
        this.username = username;
        this.profilephoto = profilephoto;
        this.blockedAt = blockedAt;
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

    public String getProfilephoto() {
        return profilephoto;
    }

    public void setProfilephoto(String profilephoto) {
        this.profilephoto = profilephoto;
    }

    public LocalDateTime getBlockedAt() {
        return blockedAt;
    }

    public void setBlockedAt(LocalDateTime blockedAt) {
        this.blockedAt = blockedAt;
    }
}
