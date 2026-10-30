package com.beem.TastyMap.rag.service;

import com.beem.TastyMap.rag.data.document.RestaurantMetadata;
import com.beem.TastyMap.rag.data.dto.ChatHistoryMessage;
import com.beem.TastyMap.rag.data.enums.MessageRole;
import com.beem.TastyMap.rag.entity.ChatMessageEntity;
import com.beem.TastyMap.rag.entity.ChatSessionEntity;
import com.beem.TastyMap.rag.repository.ChatMessageRepository;
import com.beem.TastyMap.rag.repository.ChatSessionRepository;
import com.beem.TastyMap.redis.RedisCacheService;
import com.beem.TastyMap.redis.RedisKeyGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

@Service
public class ChatMemoryManager {

    private static final Logger log = LoggerFactory.getLogger(ChatMemoryManager.class);

    private final RedisCacheService redisService;
    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;
    private final ObjectMapper objectMapper;

    private static final int MAX_CONTEXT_MESSAGES = 6;     // LLM bağlamına verilecek son mesaj penceresi
    private static final long SESSION_TTL_SECONDS = 86400; // 24 Saat (Oturum her konuştuğunda yenilenir)

    public ChatMemoryManager(RedisCacheService redisService,
                             ChatSessionRepository sessionRepo,
                             ChatMessageRepository messageRepo,
                             ObjectMapper objectMapper) {
        this.redisService = redisService;
        this.sessionRepo = sessionRepo;
        this.messageRepo = messageRepo;
        this.objectMapper = objectMapper;
    }

    private String getRedisKey(String sessionId) {
        return RedisKeyGenerator.createChatSessionKey(sessionId);
    }

    public List<RestaurantMetadata> extractLastPresentedPlaces(List<ChatHistoryMessage> history) {
        if (history == null || history.isEmpty()) return Collections.emptyList();

        for (int i = history.size() - 1; i >= 0; i--) {
            ChatHistoryMessage msg = history.get(i);
            if (msg.role() == MessageRole.ASSISTANT && msg.metadata() != null) {
                Object placesObj = msg.metadata().get("presentedPlaces");
                if (placesObj != null) {
                    try {
                        return objectMapper.convertValue(
                                placesObj,
                                new TypeReference<List<RestaurantMetadata>>() {}
                        );
                    } catch (Exception e) {
                        log.warn("[extractLastPresentedPlaces] presentedPlaces parse edilirken hata: {}", e.getMessage());
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    public Set<String> extractAllPresentedIds(List<ChatHistoryMessage> history) {
        Set<String> placeIds = new HashSet<>();
        if (history == null || history.isEmpty()) return placeIds;

        for (ChatHistoryMessage message: history) {
            if (message.role() == MessageRole.ASSISTANT && message.metadata() != null) {
                Object placesObj = message.metadata().get("presentedPlaces");
                if (placesObj != null) {
                    try {
                        List<RestaurantMetadata> list = objectMapper.convertValue(
                                placesObj,
                                new TypeReference<List<RestaurantMetadata>>() {}
                        );
                        for (RestaurantMetadata metadata: list) {
                            if (metadata != null && metadata.getQdrantId() != null) {
                                placeIds.add(metadata.getQdrantId());
                            }
                        }
                    } catch (Exception e) {
                        log.warn("[extractAllPresentedIds] presentedPlaces parse edilirken hata: {}", e.getMessage());
                    }
                }
            }
        }
        return placeIds;
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

    @Transactional
    public ChatMessageEntity recordAssistantMessage(
            String sessionId,
            Long userId,
            String content,
            List<RestaurantMetadata> places
    ) {
        Map<String, Object> metadata = (places != null && !places.isEmpty())
                ? Map.of("presentedPlaces", places)
                : Collections.emptyMap();

        return recordMessage(sessionId, userId, MessageRole.ASSISTANT, content, metadata);
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
    public ChatMessageEntity recordMessage(String sessionId, Long userId, MessageRole role, String content, Map<String, Object> metadata) {
        Instant now = Instant.now();
        Map<String, Object> safeMeta = metadata != null ? metadata : Collections.emptyMap();

        ChatMessageEntity savedEntity = saveToDatabase(sessionId, userId, role, content, safeMeta);

        // 1. Redis'e Yaz (Sliding Window & TTL)
        String key = getRedisKey(sessionId);
        ChatHistoryMessage historyItem = new ChatHistoryMessage(role, content, now, safeMeta);

        redisService.appendToListWithLimit(
                key,
                historyItem,
                MAX_CONTEXT_MESSAGES,
                SESSION_TTL_SECONDS
        );

        return savedEntity;
    }

    private ChatMessageEntity saveToDatabase(String sessionId, Long userId, MessageRole role, String content, Map<String, Object> metadata) {
        ChatSessionEntity session = sessionRepo.findById(sessionId).orElseGet(() -> {
            String cleanTitle = (content != null) ? content.replaceAll("\\s+", " ").trim() : "";
            String title = cleanTitle.isBlank() ? "Yeni Sohbet" :
                    (cleanTitle.length() > 40 ? cleanTitle.substring(0, 40) + "..." : cleanTitle);

            ChatSessionEntity newSession = new ChatSessionEntity(userId, title);
            newSession.setId(sessionId);
            return sessionRepo.saveAndFlush(newSession);
        });
        ChatMessageEntity messageEntity = new ChatMessageEntity(session, role, content, metadata);
        return messageRepo.saveAndFlush(messageEntity);
    }
}