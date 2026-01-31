package com.beem.TastyMap.UserRelated.Subscribe;

import com.beem.TastyMap.RegisterLogin.UserEntity;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscriber_id", nullable = false)
    private UserEntity subscriber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscribed_id", nullable = false)
    private UserEntity subscribed;

    @Column(name = "date", nullable = false)
    private LocalDateTime date;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserEntity getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(UserEntity subscriber) {
        this.subscriber = subscriber;
    }

    public UserEntity getSubscribed() {
        return subscribed;
    }

    public void setSubscribed(UserEntity subscribed) {
        this.subscribed = subscribed;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}
