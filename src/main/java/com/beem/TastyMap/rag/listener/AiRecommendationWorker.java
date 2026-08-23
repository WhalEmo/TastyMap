package com.beem.TastyMap.rag.listener;

import com.beem.TastyMap.rag.config.KafkaConfig;
import com.beem.TastyMap.rag.data.SearchSession;
import com.beem.TastyMap.rag.data.event.UserQueryEvent;
import com.beem.TastyMap.rag.data.response.RecommendationRes;
import com.beem.TastyMap.rag.service.RagPromptBuilder;
import com.beem.TastyMap.rag.service.SearchSessionService;
import com.beem.TastyMap.rag.service.UserDataCollectorService;
import com.beem.TastyMap.rag.service.UserDataCollectorService.UserContextData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiRecommendationWorker {

    private final VectorStore vectorStore;
    private final ChatClient.Builder chatClientBuilder;
    private final RagPromptBuilder ragPromptBuilder;
    private final SearchSessionService sessionService;
    private final UserDataCollectorService userDataCollectorService;

    private static final int PAGE_SIZE = 5;
    private static final int INITIAL_FETCH = 20;

    @KafkaListener(topics = KafkaConfig.RESTAURANT_AI_REQUESTS_TOPIC, groupId = "tasty-ai-group")
    public void processAiRequest(UserQueryEvent event) {
        Long userId = event.getUserId();
        log.info("RAG İsteği İşleniyor - Kullanıcı ID: {}, IsMoreRequest: {}", userId, event.isMoreRequest());

        SearchSession session = sessionService.getSession(userId);
        boolean isMoreRequest = event.isMoreRequest() && session != null;

        List<Document> documentsToPresent = new ArrayList<>();
        boolean isExhausted;
        int remainingCount;

        if (isMoreRequest) {
            List<String> allIds = session.getPlaceIds();
            int currentIndex = session.getCurrentIndex();

            if (currentIndex >= allIds.size()) {
                sendResponse(new RecommendationRes(userId, "Bu konumdaki tüm mekanlar zaten listelendi! 🍕", false, 0));
                return;
            }

            int nextIndex = Math.min(currentIndex + PAGE_SIZE, allIds.size());
            documentsToPresent = fetchDocumentsByIds(allIds.subList(currentIndex, nextIndex));
            sessionService.updateIndex(userId, session, nextIndex);

            remainingCount = allIds.size() - nextIndex;
            isExhausted = (remainingCount == 0);

        } else {
            // --- 2. SIFIRDAN YENİ ARAMA (DİNAMİK YARIÇAP DÖNGÜSÜ) ---
            double currentRadius = event.getRadiusKm() != null ? event.getRadiusKm() : 5.0;
            double maxRadius = 25.0; // Maksimum mesafe
            double step = 5.0;       // Her adımda artacak km

            List<Document> allResults = new ArrayList<>();

            // Mekan bulunana veya 25 km'ye ulaşılana kadar yarıçapı 5'er 5'er artır
            while (allResults.isEmpty() && currentRadius <= maxRadius) {
                log.info("Mekan aranıyor - Denenen Yarıçap: {} km", currentRadius);

                SearchRequest searchRequest = buildSearchRequest(
                        event.getUserQuery(),
                        event.getLatitude(),
                        event.getLongitude(),
                        currentRadius
                );

                allResults = vectorStore.similaritySearch(searchRequest);

                if (allResults.isEmpty()) {
                    currentRadius += step;
                }
            }

            // 25 km çapta bile hiç sonuç çıkmadıysa
            if (allResults.isEmpty()) {
                sendResponse(new RecommendationRes(userId, "Üzgünüm, kriterlerinize uygun mekan bulunamadı.", false, 0));
                return;
            }

            int firstPageEnd = Math.min(PAGE_SIZE, allResults.size());
            documentsToPresent = allResults.subList(0, firstPageEnd);

            List<String> allPlaceIds = allResults.stream().map(Document::getId).toList();
            sessionService.saveSession(userId, event.getUserQuery(), allPlaceIds);
            sessionService.updateIndex(userId, sessionService.getSession(userId), firstPageEnd);

            remainingCount = allResults.size() - firstPageEnd;
            isExhausted = (remainingCount == 0);
        }

        UserContextData userData = userDataCollectorService.collectUserData(userId);

        String systemPrompt = ragPromptBuilder.buildSystemPrompt(
                userData,
                event.isIgnoreAllergies(),
                documentsToPresent,
                event.getUserQuery(),
                isExhausted,
                remainingCount
        );

        String aiResponse = chatClientBuilder.build()
                .prompt()
                .user(systemPrompt)
                .call()
                .content();

        sendResponse(new RecommendationRes(userId, aiResponse, !isExhausted, remainingCount));
    }

    private SearchRequest buildSearchRequest(String query, Double lat, Double lon, double radiusKm) {
        SearchRequest request = SearchRequest.query(query).withTopK(INITIAL_FETCH);
        if (lat != null && lon != null) {
            request = request.withFilterExpression(
                    String.format("location NEAR { lat: %f, lon: %f, distance: '%fkm' }", lat, lon, radiusKm)
            );
        }
        return request;
    }

    private List<Document> fetchDocumentsByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        String formattedIds = ids.stream()
                .map(id -> "'" + id + "'")
                .toList()
                .toString();

        return vectorStore.similaritySearch(
                SearchRequest.query("*")
                        .withTopK(ids.size())
                        .withFilterExpression(String.format("id IN %s", formattedIds))
        );
    }

    private void sendResponse(RecommendationRes response) {
        log.info("--- YANIT GÖNDERİLDİ (User: {}, HasMore: {}) ---", response.getUserId(), response.isHasMore());
        // Frontend / Gateway iletimi
    }
}