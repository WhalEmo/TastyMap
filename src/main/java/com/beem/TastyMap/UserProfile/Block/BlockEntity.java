package com.beem.TastyMap.UserProfile.Block;

import com.beem.TastyMap.RegisterLogin.UserEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
@Entity
@Table(
        name = "user_blocks",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"blocker_id", "blocked_id"})
        },
        indexes = {
                @Index(name = "idx_blocker", columnList = "blocker_id"),
                @Index(name = "idx_blocked", columnList = "blocked_id")
        }
)
public class BlockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // engelleyen kişi
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private UserEntity blocker;

    // engellenen kişi
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private UserEntity blocked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserEntity getBlocker() {
        return blocker;
    }

    public void setBlocker(UserEntity blocker) {
        this.blocker = blocker;
    }

    public UserEntity getBlocked() {
        return blocked;
    }

    public void setBlocked(UserEntity blocked) {
        this.blocked = blocked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
