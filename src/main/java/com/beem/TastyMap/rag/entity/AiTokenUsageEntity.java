package com.beem.TastyMap.rag.entity;

import com.beem.TastyMap.rag.data.enums.AiProvider;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_token_usages", indexes = {
        @Index(name = "idx_usage_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_usage_session", columnList = "session_id"),
        @Index(name = "idx_usage_message", columnList = "message_id")
})
public class AiTokenUsageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Oturumla ilişki (Oturum silinirse muhasebe kaydı patlamasın diye optional=true)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", foreignKey = @ForeignKey(name = "fk_token_usage_session"))
    private ChatSessionEntity session;

    // İlgili asistan mesajı ile ilişki
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", foreignKey = @ForeignKey(name = "fk_token_usage_message"))
    private ChatMessageEntity message;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32)
    private AiProvider provider; // GROQ, EVREN, OLLAMA

    @Column(name = "model_name", nullable = false, length = 64)
    private String modelName; // qwen/qwen3.8-flash-next, qwen/qwen3.8-27b

    @Column(name = "prompt_tokens", nullable = false)
    private Integer promptTokens = 0;

    @Column(name = "completion_tokens", nullable = false)
    private Integer completionTokens = 0;

    @Column(name = "total_tokens", nullable = false)
    private Integer totalTokens = 0;

    // Dolar maliyeti (Örn: $0.000142)
    @Column(name = "cost_usd", nullable = false, precision = 12, scale = 8)
    private BigDecimal costUsd = BigDecimal.ZERO;

    // TL maliyeti (Örn: 0.006390 TL)
    @Column(name = "cost_try", nullable = false, precision = 12, scale = 6)
    private BigDecimal costTry = BigDecimal.ZERO;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs = 0L;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AiTokenUsageEntity() {}

    public AiTokenUsageEntity(Long userId, ChatSessionEntity session, ChatMessageEntity message,
                              AiProvider provider, String modelName, int promptTokens,
                              int completionTokens, BigDecimal costUsd, BigDecimal costTry, long durationMs) {
        this.userId = userId;
        this.session = session;
        this.message = message;
        this.provider = provider;
        this.modelName = modelName;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = promptTokens + completionTokens;
        this.costUsd = costUsd;
        this.costTry = costTry;
        this.durationMs = durationMs;
        this.createdAt = LocalDateTime.now();
    }

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public ChatSessionEntity getSession() { return session; }
    public void setSession(ChatSessionEntity session) { this.session = session; }
    public ChatMessageEntity getMessage() { return message; }
    public void setMessage(ChatMessageEntity message) { this.message = message; }
    public AiProvider getProvider() { return provider; }
    public void setProvider(AiProvider provider) { this.provider = provider; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public Integer getPromptTokens() { return promptTokens; }
    public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }
    public Integer getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }
    public Integer getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
    public BigDecimal getCostUsd() { return costUsd; }
    public void setCostUsd(BigDecimal costUsd) { this.costUsd = costUsd; }
    public BigDecimal getCostTry() { return costTry; }
    public void setCostTry(BigDecimal costTry) { this.costTry = costTry; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}