package com.beem.TastyMap.rag.service;

import com.beem.TastyMap.rag.data.dto.ChatHistoryMessage;
import com.beem.TastyMap.rag.data.enums.MessageRole;
import com.beem.TastyMap.rag.entity.ChatMessageEntity;
import com.beem.TastyMap.rag.entity.ChatSessionEntity;
import com.beem.TastyMap.rag.repository.ChatMessageRepository;
import com.beem.TastyMap.rag.repository.ChatSessionRepository;
import com.beem.TastyMap.redis.RedisCacheService;
import com.beem.TastyMap.redis.RedisKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ChatMemoryManager {

    private static final Logger log = LoggerFactory.getLogger(ChatMemoryManager.class);

    private final RedisCacheService redisService;
    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;

    private static final int MAX_CONTEXT_MESSAGES = 6;     // LLM bağlamına verilecek son mesaj penceresi
    private static final long SESSION_TTL_SECONDS = 86400; // 24 Saat (Oturum her konuştuğunda yenilenir)

    public ChatMemoryManager(RedisCacheService redisService,
                             ChatSessionRepository sessionRepo,
                             ChatMessageRepository messageRepo) {
        this.redisService = redisService;
        this.sessionRepo = sessionRepo;
        this.messageRepo = messageRepo;
    }

    private String getRedisKey(String sessionId) {
        return RedisKeyGenerator.createChatSessionKey(sessionId);
    }

    /**
     * Oturumun son mesajlarını kronolojik sırayla döner.
     * Cache-Aside: Önce Redis'e bakar, yoksa MySQL'den çekip Redis'i ısıtır (Warm-up).
     */
    public List<ChatHistoryMessage> getRecentMessages(String sessionId) {
        String key = getRedisKey(sessionId);

        // 1. Sıcak Katman (Redis) Kontrolü
        List<ChatHistoryMessage> cached = redisService.getList(key, ChatHistoryMessage.class);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        // 2. Soğuk Katman (MySQL): Son MAX_CONTEXT_MESSAGES adet mesajı çek
        List<ChatMessageEntity> dbMessages = messageRepo.findBySession_IdOrderByCreatedAtDesc(
                sessionId, PageRequest.of(0, MAX_CONTEXT_MESSAGES));

        if (dbMessages.isEmpty()) {
            return Collections.emptyList();
        }

        // DB'den tersten (yeni -> eski) geldiği için kronolojik sıraya (eski -> yeni) çevir
        Collections.reverse(dbMessages);

        List<ChatHistoryMessage> history = dbMessages.stream()
                .map(m -> new ChatHistoryMessage(
                        m.getRole(),
                        m.getContent(),
                        m.getCreatedAt() != null
                                ? m.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()
                                : Instant.now(),
                        m.getMetadata() != null ? m.getMetadata() : Collections.emptyMap()
                ))
                .toList();

        // 3. Redis'i Isıt (Warm-Up): MySQL'den çekilenleri Redis listesine besle
        for (ChatHistoryMessage msg : history) {
            redisService.appendToListWithLimit(key, msg, MAX_CONTEXT_MESSAGES, SESSION_TTL_SECONDS);
        }

        return history;
    }

    /**
     * Mesajı hem Redis sıcak önbelleğine hem de MySQL kalıcı tablosuna işler.
     */
    public void recordMessage(String sessionId, Long userId, MessageRole role, String content) {
        recordMessage(sessionId, userId, role, content, Collections.emptyMap());
    }

    /**
     * Metadata destekli aşırı yüklenmiş (overloaded) kayıt metodu.
     */
    @Transactional
    public void recordMessage(String sessionId, Long userId, MessageRole role, String content, Map<String, Object> metadata) {
        Instant now = Instant.now();
        Map<String, Object> safeMeta = metadata != null ? metadata : Collections.emptyMap();

        // 1. Redis'e Yaz (Sliding Window & TTL)
        String key = getRedisKey(sessionId);
        ChatHistoryMessage historyItem = new ChatHistoryMessage(role, content, now, safeMeta);

        redisService.appendToListWithLimit(
                key,
                historyItem,
                MAX_CONTEXT_MESSAGES,
                SESSION_TTL_SECONDS
        );

        // 2. MySQL'e Kaydet
        saveToDatabase(sessionId, userId, role, content, safeMeta);
    }

    private void saveToDatabase(String sessionId, Long userId, MessageRole role, String content, Map<String, Object> metadata) {
        try {
            // Oturum yoksa ilk mesajın özetiyle başlık oluşturarak başlat
            ChatSessionEntity session = sessionRepo.findById(sessionId).orElseGet(() -> {
                String cleanTitle = (content != null) ? content.replaceAll("\\s+", " ").trim() : "";

                String title = cleanTitle.isBlank() ? "Yeni Sohbet" :
                        (cleanTitle.length() > 40 ? cleanTitle.substring(0, 40) + "..." : cleanTitle);

                ChatSessionEntity newSession = new ChatSessionEntity(userId, title);
                newSession.setId(sessionId);
                return sessionRepo.save(newSession);
            });

            // İlişkili nesne referansı ile kayıt
            ChatMessageEntity messageEntity = new ChatMessageEntity(session, role, content, metadata);
            messageRepo.save(messageEntity);

        } catch (Exception e) {
            log.error("Mesaj DB'ye kaydedilirken hata oluştu (Session: {}): {}", sessionId, e.getMessage());
        }
    }
}