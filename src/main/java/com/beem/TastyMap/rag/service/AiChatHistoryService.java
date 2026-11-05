package com.beem.TastyMap.rag.service;

import com.beem.TastyMap.maps.entity.PlaceEntity;
import com.beem.TastyMap.maps.repository.PlaceRepo;
import com.beem.TastyMap.rag.data.document.RestaurantMetadata;
import com.beem.TastyMap.rag.data.enums.MessageRole;
import com.beem.TastyMap.rag.data.response.AiChatMessageResponse;
import com.beem.TastyMap.rag.data.response.AiSessionSummaryResponse;
import com.beem.TastyMap.rag.data.response.HydratedPlaceCardDto;
import com.beem.TastyMap.rag.entity.ChatMessageEntity;
import com.beem.TastyMap.rag.entity.ChatSessionEntity;
import com.beem.TastyMap.rag.repository.ChatMessageRepository;
import com.beem.TastyMap.rag.repository.ChatSessionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AiChatHistoryService {

    private static final Logger log = LoggerFactory.getLogger(AiChatHistoryService.class);

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final PlaceRepo placeRepository;
    private final ObjectMapper objectMapper;

    public AiChatHistoryService(ChatSessionRepository sessionRepository,
                                ChatMessageRepository messageRepository,
                                PlaceRepo placeRepository,
                                ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.placeRepository = placeRepository;
        this.objectMapper = objectMapper;
    }

    // 1. Querydsl + Slice ile Oturum Listesi
    @Transactional(readOnly = true)
    public Slice<AiSessionSummaryResponse> getUserSessions(Long userId, Pageable pageable) {
        return sessionRepository.findSessionsByUserId(userId, pageable);
    }

    // 2. Querydsl Join ile Güvenlik + Mesaj Çekimi + Hydration
    @Transactional(readOnly = true)
    public List<AiChatMessageResponse> getSessionMessages(String sessionId, Long userId) {
        List<ChatMessageEntity> messages = messageRepository.findMessagesBySessionAndUser(sessionId, userId);
        if (messages.isEmpty()) {
            return Collections.emptyList();
        }

        // Tüm asistan mesajlarındaki mekan ID'lerini topla (N+1 engelleme)
        Set<Long> allPlaceDbIds = new HashSet<>();
        for (ChatMessageEntity msg : messages) {
            if (msg.getRole() == MessageRole.ASSISTANT && msg.getMetadata() != null) {
                List<RestaurantMetadata> metadataList = extractPresentedPlaces(msg.getMetadata());
                for (RestaurantMetadata metadata : metadataList) {
                    if (metadata.getDbId() != null) {
                        allPlaceDbIds.add(metadata.getDbId());
                    }
                }
            }
        }

        // Querydsl ile mekanları tek sorguda çek ve Map yap
        Map<Long, PlaceEntity> livePlacesMap = placeRepository.findHydrationPlacesByIds(allPlaceDbIds);

        return messages.stream()
                .map(msg -> mapToHydratedResponse(msg, livePlacesMap))
                .toList();
    }

    @Transactional
    public void deleteSession(String sessionId, Long userId) {
        ChatSessionEntity session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Silinecek oturum bulunamadı veya yetkiniz yok!"));
        sessionRepository.delete(session);
    }

    private AiChatMessageResponse mapToHydratedResponse(ChatMessageEntity msg, Map<Long, PlaceEntity> livePlacesMap) {
        List<HydratedPlaceCardDto> hydratedPlaces = Collections.emptyList();

        if (msg.getRole() == MessageRole.ASSISTANT && msg.getMetadata() != null) {
            List<RestaurantMetadata> metadataList = extractPresentedPlaces(msg.getMetadata());
            if (!metadataList.isEmpty()) {
                hydratedPlaces = new ArrayList<>();
                for (RestaurantMetadata metadata : metadataList) {
                    Long dbId = metadata.getDbId();
                    PlaceEntity liveEntity = (dbId != null) ? livePlacesMap.get(dbId) : null;

                    // Null güvenliği: highlightedFoods null gelirse boş liste ver
                    List<String> highlightedFoods = metadata.getHighlightedFoods() != null
                            ? metadata.getHighlightedFoods()
                            : Collections.emptyList();

                    if (liveEntity != null) {
                        Boolean isOpenNow = (liveEntity.getOpeningHours() != null)
                                ? liveEntity.getOpeningHours().getOpenNow()
                                : null;

                        String address = liveEntity.getVicinity() != null
                                ? liveEntity.getVicinity()
                                : liveEntity.getFormattedAddress();

                        hydratedPlaces.add(new HydratedPlaceCardDto(
                                liveEntity.getId(),
                                liveEntity.getPlaceId(),
                                liveEntity.getName(),
                                address,
                                liveEntity.getLatitude(),
                                liveEntity.getLongitude(),
                                liveEntity.getGoogleRating(),
                                liveEntity.getTastyMapRating(),
                                liveEntity.getPriceLevel(),
                                isOpenNow,
                                liveEntity.getBusinessStatus(),
                                highlightedFoods,
                                true
                        ));
                    } else {
                        // DB'de mekan bulunamazsa snapshot metadatayı koru (temiz fallback)
                        hydratedPlaces.add(new HydratedPlaceCardDto(
                                dbId,
                                metadata.getPlaceId(),
                                metadata.getName() != null ? metadata.getName() : "Bilinmeyen Mekan",
                                null,
                                metadata.getLatitude(),
                                metadata.getLongitude(),
                                metadata.getGoogleRating(),
                                metadata.getTastymapRating(),
                                null,
                                null,
                                "UNAVAILABLE",
                                highlightedFoods,
                                false
                        ));
                    }
                }
            }
        }

        return new AiChatMessageResponse(
                msg.getId(),
                msg.getRole(),
                msg.getContent(),
                msg.getCreatedAt(),
                hydratedPlaces
        );
    }

    private List<RestaurantMetadata> extractPresentedPlaces(Map<String, Object> metadata) {
        Object presentedObj = metadata.get("presentedPlaces");
        if (presentedObj == null) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.convertValue(
                    presentedObj,
                    new TypeReference<List<RestaurantMetadata>>() {}
            );
        } catch (Exception e) {
            log.warn("[extractPresentedPlaces] presentedPlaces parse hatası: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}