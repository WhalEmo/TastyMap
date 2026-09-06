package com.beem.TastyMap.userRelated.socialnotifications;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.userRelated.socialnotifications.enums.NotificationActionStatus;
import com.beem.TastyMap.userRelated.socialnotifications.enums.SocialNotificationType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "social_notifications",
        indexes = {
                @Index(name = "idx_recipient", columnList = "recipient_id"),
                @Index(name = "idx_recipient_read", columnList = "recipient_id, isRead")
        }
)
public class SocialNotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Bildiirmi alan kisi
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private UserEntity recipient;

    // Bildirimi tetikleyen kisi
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private UserEntity actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialNotificationType type;

    @Enumerated(EnumType.STRING)
    private NotificationActionStatus actionStatus = NotificationActionStatus.NONE;

    // İsteğe bağlı: Hangi post veya yorum? (Eğer takip isteğiyse null olur)
    @Column(name = "target_id")
    private Long targetId;

    // Bildirimin içeriği (Örn: yorum metninin ilk 30 karakteri)
    private String content;

    private boolean isRead = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserEntity getRecipient() {
        return recipient;
    }

    public void setRecipient(UserEntity recipient) {
        this.recipient = recipient;
    }

    public UserEntity getActor() {
        return actor;
    }

    public void setActor(UserEntity actor) {
        this.actor = actor;
    }

    public SocialNotificationType getType() {
        return type;
    }

    public void setType(SocialNotificationType type) {
        this.type = type;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public NotificationActionStatus getActionStatus() {
        return actionStatus;
    }

    public void setActionStatus(NotificationActionStatus actionStatus) {
        this.actionStatus = actionStatus;
    }
}