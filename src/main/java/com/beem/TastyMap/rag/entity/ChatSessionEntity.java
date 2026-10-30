package com.beem.TastyMap.rag.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chat_sessions")
public class ChatSessionEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id; // UUID

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", length = 255)
    private String title;

    // Oturum silindiğinde tüm mesajları da DB'den otomatik temizlensin (orphanRemoval)
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ChatMessageEntity> messages = new ArrayList<>();

    @Column(name = "total_tokens", nullable = false)
    private Integer totalTokens = 0;

    @Column(name = "total_cost_usd", nullable = false, precision = 10, scale = 4)
    private BigDecimal totalCostUsd = BigDecimal.ZERO;

    @Column(name = "total_cost_try", nullable = false, precision = 10, scale = 4)
    private BigDecimal totalCostTry = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ChatSessionEntity() {}

    public ChatSessionEntity(Long userId, String title) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.title = title;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Mesaj eklemeyi kolaylaştıran yardımcı metod
    public void addMessage(ChatMessageEntity message) {
        messages.add(message);
        message.setSession(this);
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getter & Setter'lar
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public List<ChatMessageEntity> getMessages() { return messages; }
    public void setMessages(List<ChatMessageEntity> messages) { this.messages = messages; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    public BigDecimal getTotalCostUsd() {
        return totalCostUsd;
    }

    public void setTotalCostUsd(BigDecimal totalCostUsd) {
        this.totalCostUsd = totalCostUsd;
    }

    public BigDecimal getTotalCostTry() {
        return totalCostTry;
    }

    public void setTotalCostTry(BigDecimal totalCostTry) {
        this.totalCostTry = totalCostTry;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}