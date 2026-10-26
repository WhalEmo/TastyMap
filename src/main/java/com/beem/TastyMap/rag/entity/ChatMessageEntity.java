package com.beem.TastyMap.rag.entity;

import com.beem.TastyMap.rag.data.enums.MessageRole;
import com.beem.TastyMap.rag.entity.converter.JsonToMapConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_messages_session_created", columnList = "session_id, created_at")
})
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Gerçek Foreign Key ilişkisi
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, foreignKey = @ForeignKey(name = "fk_chat_messages_session"))
    private ChatSessionEntity session;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MessageRole role; // USER, ASSISTANT

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Convert(converter = JsonToMapConverter.class)
    @Column(name = "metadata", columnDefinition = "TEXT")
    private Map<String, Object> metadata = Collections.emptyMap();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ChatMessageEntity() {}

    public ChatMessageEntity(ChatSessionEntity session, MessageRole role, String content, Map<String, Object> metadata) {
        this.session = session;
        this.role = role;
        this.content = content;
        this.metadata = (metadata != null) ? metadata : Collections.emptyMap();
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ChatSessionEntity getSession() {
        return session;
    }

    public void setSession(ChatSessionEntity session) {
        this.session = session;
    }

    public MessageRole getRole() {
        return role;
    }

    public void setRole(MessageRole role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}