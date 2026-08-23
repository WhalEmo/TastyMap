package com.beem.TastyMap.rag.listener;

import com.beem.TastyMap.rag.config.KafkaConfig;
import com.beem.TastyMap.rag.data.SearchSession;
import com.beem.TastyMap.rag.data.event.UserQueryEvent;
import com.beem.TastyMap.rag.data.response.RecommendationRes;
import com.beem.TastyMap.rag.service.RagPromptBuilder;
import com.beem.TastyMap.rag.service.SearchSessionService;
import com.beem.TastyMap.userRelated.visit.VisitResponseDTO;
import com.beem.TastyMap.userRelated.visit.VisitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.data.domain.Page;
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
    private final VisitService visitService;
    private final SearchSessionService sessionService; // Redis Oturum Servisi Eklendi

    private static final int PAGE_SIZE = 5;      // Kullanıcıya her adımda sunulacak mekan sayısı
    private static final int INITIAL_FETCH = 20; // Qdrant'tan ilk aramada çekilecek toplam mekan sayısı

    @KafkaListener(topics = KafkaConfig.RESTAURANT_AI_REQUESTS_TOPIC, groupId = "tasty-ai-group")
    public void processAiRequest(UserQueryEvent event) {
        log.info("Kafka'dan yeni RAG isteği alındı. Kullanıcı ID: {}, IsMoreRequest: {}", event.getUserId(), event.isMoreRequest());

        Long userId = event.getUserId();
        SearchSession session = sessionService.getSession(userId);

        // Kullanıcı butona bastıysa VE aktif bir oturumu varsa 'daha fazla' moduna geç
        boolean isMoreRequest = event.isMoreRequest() && session != null;

        List<Document> documentsToPresent = new ArrayList<>();
        boolean isExhausted;
        int remainingCount;

        if (isMoreRequest) {
            // --- 1. DAHA FAZLA İSTEĞİ (REDİS'TEN SIRADAKİ MEKANLARI OKUMA) ---
            List<String> allIds = session.getPlaceIds();
            int currentIndex = session.getCurrentIndex();

            if (currentIndex >= allIds.size()) {
                sendResponse(new RecommendationRes(
                        userId,
                        "Bu konumda aradığınız kriterlere uygun tüm mekanları zaten sundum! 🍕 İsterseniz farklı bir yemek veya semt arayabiliriz.",
                        false,
                        0
                ));
                return;
            }

            int nextIndex = Math.min(currentIndex + PAGE_SIZE, allIds.size());
            List<String> pageIds = allIds.subList(currentIndex, nextIndex);

            documentsToPresent = fetchDocumentsByIds(pageIds);
            sessionService.updateIndex(userId, session, nextIndex);

            remainingCount = allIds.size() - nextIndex;
            isExhausted = (remainingCount == 0);

        } else {
            // --- 2. SIFIRDAN YENİ ARAMA (QDRANT'TAN MEKAN ÇEKME) ---
            SearchRequest searchRequest = SearchRequest.query(event.getUserQuery()).withTopK(INITIAL_FETCH);

            if (event.getLatitude() != null && event.getLongitude() != null) {
                double radius = event.getRadiusKm() != null ? event.getRadiusKm() : 5.0; // Varsayılan 5km

                searchRequest = searchRequest.withFilterExpression(
                        String.format("location NEAR { lat: %f, lon: %f, distance: '%fkm' }",
                                event.getLatitude(),
                                event.getLongitude(),
                                radius)
                );
            }

            List<Document> allResults = vectorStore.similaritySearch(searchRequest);

            if (allResults.isEmpty()) {
                sendResponse(new RecommendationRes(
                        userId,
                        "Üzgünüm, belirttiğiniz konumda ve kriterlerde herhangi bir mekan bulunamadı.",
                        false,
                        0
                ));
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

        // --- 3. ZİYARET GEÇMİŞİNİ ÇEK ---
        Page<VisitResponseDTO> visitPage = visitService.getVisits(userId, 0, 10);
        List<VisitResponseDTO> recentVisits = visitPage != null ? visitPage.getContent() : List.of();

        // --- 4. SYSTEM PROMPT OLUŞTURMA ---
        String systemPrompt = ragPromptBuilder.buildSystemPrompt(
                userId,
                event.isIgnoreAllergies(),
                documentsToPresent,
                event.getUserQuery(),
                recentVisits,
                isExhausted,
                remainingCount
        );

        // --- 5. LLM YANITI ÜRETME ---
        String aiResponse = chatClientBuilder.build()
                .prompt()
                .user(systemPrompt)
                .call()
                .content();

        // --- 6. KULLANICIYA VE FRONTEND'E YANITI GÖNDERME ---
        RecommendationRes responseDTO = new RecommendationRes(
                userId,
                aiResponse,
                !isExhausted, // Kalan mekan varsa frontend'de buton göster (hasMore)
                remainingCount
        );

        sendResponse(responseDTO);
    }

    private List<Document> fetchDocumentsByIds(List<String> ids) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        return vectorStore.similaritySearch(
                SearchRequest.query("*")
                        .withTopK(ids.size())
                        .withFilterExpression(b.in("place_id", ids.toArray()).build())
        );
    }

    private void sendResponse(RecommendationRes response) {
        log.info("--- AI YANITI ÜRETİLDİ (Kullanıcı ID: {}, HasMore: {}, Kalan: {}) ---",
                response.getUserId(), response.isHasMore(), response.getRemainingCount());
        log.info(response.getAiMessage());
        // WebSocket / Push Notification ile frontend'e responseDTO iletilir.
    }
}