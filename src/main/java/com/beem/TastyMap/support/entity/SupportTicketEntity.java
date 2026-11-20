package com.beem.TastyMap.support.entity;

import com.beem.TastyMap.support.model.SupportCategory;
import com.beem.TastyMap.support.model.SupportStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_tickets", indexes = {
        @Index(name = "idx_support_user_id", columnList = "user_id"),
        @Index(name = "idx_support_status", columnList = "status")
})
public class SupportTicketEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SupportCategory category;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SupportStatus status = SupportStatus.OPEN;

    @Column(name = "app_version", length = 20)
    private String appVersion;

    @Column(name = "device_info", length = 150)
    private String deviceInfo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public SupportCategory getCategory() { return category; }
    public void setCategory(SupportCategory category) { this.category = category; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public SupportStatus getStatus() { return status; }
    public void setStatus(SupportStatus status) { this.status = status; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}