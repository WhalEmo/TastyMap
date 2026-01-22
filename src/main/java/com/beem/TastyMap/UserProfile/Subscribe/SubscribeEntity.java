package com.beem.TastyMap.UserProfile.Subscribe;

import jakarta.persistence.*;

import java.time.LocalDateTime;
@Entity
@Table(
        name = "subscribe",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"subscriber_id", "subscribed_id"})
        },
        indexes = {
                @Index(name = "idx_subscriber", columnList = "subscriber_id"),
                @Index(name = "idx_subscribed", columnList = "subscribed_id")
        }
)
public class SubscribeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscriber_id", nullable = false)
    private Long subscriberId;

    @Column(name = "subscribed_id", nullable = false)
    private Long subscribedId;

    @Column(name = "date", nullable = false)
    private LocalDateTime date;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(Long subscriberId) {
        this.subscriberId = subscriberId;
    }

    public Long getSubscribedId() {
        return subscribedId;
    }

    public void setSubscribedId(Long subscribedId) {
        this.subscribedId = subscribedId;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}
