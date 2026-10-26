package com.beem.TastyMap.rag.listener;

import com.beem.TastyMap.rag.component.PlaceMetadataExtractor;
import com.beem.TastyMap.rag.data.dto.ChatHistoryMessage;
import com.beem.TastyMap.rag.data.dto.PresentedPlaceSummary;
import com.beem.TastyMap.rag.data.dto.SearchResult;
import com.beem.TastyMap.rag.data.dto.UserIntentAnalysis;
import com.beem.TastyMap.rag.data.enums.MessageRole;
import com.beem.TastyMap.rag.data.event.UserQueryEvent;
import com.beem.TastyMap.rag.data.response.AiStreamPayload;
import com.beem.TastyMap.rag.data.response.AiStreamPayload.StreamEventType;
import com.beem.TastyMap.rag.factory.AiProviderFactory;
import com.beem.TastyMap.rag.service.ChatMemoryManager;
import com.beem.TastyMap.rag.service.RestaurantSearchService;
import com.beem.TastyMap.rag.util.CostTracker;
import com.beem.TastyMap.rag.websocket.AiStreamWebSocketHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class AiRecommendationWorkerV2 {

    private static final Logger log = LoggerFactory.getLogger(AiRecommendationWorkerV2.class);

    private static final Duration STREAM_TIMEOUT = Duration.ofSeconds(240);
    private static final int DEFAULT_HISTORY_ROUNDS = 2;
    private static final double DEFAULT_RADIUS_KM = 5.0;

    private final AiProviderFactory aiProviderFactory;
    private final ChatMemoryManager chatMemoryManager;
    private final AiStreamWebSocketHandler webSocketHandler;
    private final RestaurantSearchService restaurantSearchService;
    private final PlaceMetadataExtractor metadataExtractor;
    private final ObjectMapper objectMapper;

    // 1. AŞAMA İÇİN NET JSON EXTRACTION PROMPT'U
    private static final String INTENT_SYSTEM_PROMPT = """
    Sen TastyMap sisteminin niyet analiz motorusun.
    Kullanıcının mesajını incele ve SADECE aşağıdaki JSON formatında yanıt ver.
    Açıklama, markdown veya selamlama EKLEME.

    KURALLAR:
    1. Kullanıcı doğrudan belirli bir mekan/dükkan adı soruyorsa (örn: "Ay Işığı Kafe", "Döner House", "Koff Coffee"):
       - search_needed: true
       - is_named_entity: true
       - semantic_query: "Mekanın adı ve varsa spesifik arama terimi"
       - direct_reply: ""

    2. Kullanıcı genel bir lezzet, mekan veya kategori arıyorsa (örn: "sessiz kafe", "yakında dönerci", "ders çalışmalık yer"):
       - search_needed: true
       - is_named_entity: false
       - semantic_query: "aranacak temiz anahtar kelimeler"
       - direct_reply: ""

    3. Kullanıcı sadece selam veriyor, hal hatır soruyor veya genel sohbet ediyorsa:
       - search_needed: false
       - is_named_entity: false
       - semantic_query: ""
       - direct_reply: "Samimi ve kısa cevabın"

    ÖRNEK JSON:
    {
      "search_needed": true,
      "is_named_entity": false,
      "semantic_query": "sessiz ders calismalik kafe",
      "direct_reply": ""
    }
    """;

    // 2. AŞAMA İÇİN STREAMING TANITIM KURALLARI
    private static final String PRESENTATION_SYSTEM_PROMPT = """
        Sen TastyMap'in samimi ve uzman mekan rehberisin.

        KURALLAR:
        1. YALNIZCA sana verilen mekan listesindeki bilgileri (lezzet, ambiyans, dikkat edilecekler) kullan. Listede olmayan mekan veya bilgi uydurma.
        2. Her mekanı akıcı ve net bir paragrafla tanıt. Mekan adını kalın (**Mekan Adı**) yaz.
        3. Mekanın öne çıkan lezzetlerini ve ortamını vurgula.
        4. "Dikkat Edilmesi Gerekenler" kısmında olumsuz bir not varsa (sıra bekleme, klima vb.) kullanıcıyı dostça uyar.
        5. Giriş ve kapanış cümlelerin samimi ve enerjik olsun.
        """;

    public AiRecommendationWorkerV2(
            AiProviderFactory aiProviderFactory,
            ChatMemoryManager chatMemoryManager,
            AiStreamWebSocketHandler webSocketHandler,
            RestaurantSearchService restaurantSearchService,
            PlaceMetadataExtractor metadataExtractor,
            ObjectMapper objectMapper
    ) {
        this.aiProviderFactory = aiProviderFactory;
        this.chatMemoryManager = chatMemoryManager;
        this.webSocketHandler = webSocketHandler;
        this.restaurantSearchService = restaurantSearchService;
        this.metadataExtractor = metadataExtractor;
        this.objectMapper = objectMapper;
    }

    public void processAiRequest(UserQueryEvent event) {
        Long userId = event.userId();
        String sessionId = (event.sessionId() != null && !event.sessionId().isBlank())
                ? event.sessionId()
                : UUID.randomUUID().toString();

        long startTime = System.currentTimeMillis();
        log.info("[V2 Worker] RAG İsteği Başladı -> User: {}, Session: {}, Query: '{}'", userId, sessionId, event.userQuery());

        notifyUser(userId, StreamEventType.STATUS, "İsteğiniz inceleniyor... 🔍", false, 0);

        // 1. Geçmişi al ve kullanıcı mesajını DB'ye yaz
        List<ChatHistoryMessage> fullHistory = chatMemoryManager.getRecentMessages(sessionId);
        recordUserMessage(sessionId, userId, event);

        Set<String> alreadyPresentedIds = extractAllPresentedIds(fullHistory);
        List<PresentedPlaceSummary> newlyFoundPlaces = new ArrayList<>();
        StringBuilder fullResponse = new StringBuilder();

        try {
            ChatClient client = aiProviderFactory.getDefaultClient();

            ExecutionTokenUsage intentTokens = new ExecutionTokenUsage();
            UserIntentAnalysis intent = analyzeUserIntent(client, event.userQuery(), intentTokens);

            log.info("[V2 Worker] 1. Aşama: Niyet ve arama analizi yapılıyor...");

            if (intent.searchNeeded()) {
                newlyFoundPlaces = executeSearchAndStream(
                        client, event, sessionId, userId, intent, alreadyPresentedIds, fullHistory, fullResponse, intentTokens
                );
            } else {
                handleDirectReply(userId, intent, fullResponse);
            }

            notifyUser(userId, StreamEventType.COMPLETED, "", false, 0);
            recordAssistantMessage(sessionId, userId, fullResponse.toString(), newlyFoundPlaces);

            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("[V2 Worker] İşlem Tamamlandı. Toplam Süre: {} ms, Sunulan: {}", totalDuration, newlyFoundPlaces.size());
        } catch (Exception e) {
            log.error("[V2 Worker] Kritik Hata: {}", e.getMessage(), e);
            notifyUser(userId, StreamEventType.ERROR, "Tavsiyeler hazırlanırken bir sorun oluştu.", false, 0);
        }
    }

    private void recordUserMessage(String sessionId, Long userId, UserQueryEvent event) {
        Map<String, Object> userMeta = new HashMap<>();
        if (event.latitude() != null && event.longitude() != null) {
            userMeta.put("lat", event.latitude());
            userMeta.put("lon", event.longitude());
        }
        chatMemoryManager.recordMessage(sessionId, userId, MessageRole.USER, event.userQuery(), userMeta);
    }

    private void recordAssistantMessage(String sessionId, Long userId, String content, List<PresentedPlaceSummary> places) {
        Map<String, Object> assistantMeta = new HashMap<>();
        if (!places.isEmpty()) {
            assistantMeta.put("presentedPlaces", new ArrayList<>(places));
        }
        chatMemoryManager.recordMessage(sessionId, userId, MessageRole.ASSISTANT, content, assistantMeta);
    }

    private void handleDirectReply(Long userId, UserIntentAnalysis intent, StringBuilder fullResponse) {
        String reply = (intent.directReply() != null && !intent.directReply().isBlank())
                ? intent.directReply()
                : "Merhaba! Size en lezzetli mekanları bulmak için buradayım. Ne arzu edersiniz?";
        fullResponse.append(reply);
        notifyUser(userId, StreamEventType.TOKEN, reply, false, 0);
    }

    private UserIntentAnalysis analyzeUserIntent(ChatClient client, String userQuery, ExecutionTokenUsage tokenUsage) {
        log.info("[V2 Worker] 1. Aşama: Niyet analizi yapılıyor...");

        Prompt intentPrompt = new Prompt(List.of(
                new SystemMessage(INTENT_SYSTEM_PROMPT),
                new UserMessage("Kullanıcı İletisi: " + userQuery)
        ));

        ChatResponse response = client.prompt(intentPrompt).call().chatResponse();
        String rawOutput = (response != null && response.getResult() != null)
                ? response.getResult().getOutput().getContent()
                : "";

        if (response != null && response.getMetadata() != null && response.getMetadata().getUsage() != null) {
            var usage = response.getMetadata().getUsage();
            tokenUsage.inputTokens.set(usage.getPromptTokens() != null ? usage.getPromptTokens() : 0);
            tokenUsage.outputTokens.set(usage.getGenerationTokens() != null ? usage.getGenerationTokens() : 0);
            CostTracker.logSummary("1. Aşama (Niyet Analizi)", tokenUsage.inputTokens.get(), tokenUsage.outputTokens.get());
        }

        log.info("[V2 Worker] Niyet Çıktısı: {}", rawOutput);
        return parseIntentResponse(rawOutput, userQuery);
    }

    private List<PresentedPlaceSummary> executeSearchAndStream(
            ChatClient client,
            UserQueryEvent event,
            String sessionId,
            Long userId,
            UserIntentAnalysis intent,
            Set<String> alreadyPresentedIds,
            List<ChatHistoryMessage> fullHistory,
            StringBuilder fullResponse,
            ExecutionTokenUsage intentTokens
    ) {
        String searchQuery = (intent.semanticQuery() != null && !intent.semanticQuery().isBlank())
                ? intent.semanticQuery()
                : event.userQuery();

        log.info("🔍 [V2 Worker] Qdrant Arama Tetikleniyor -> Query: '{}', Lat: {}, Lon: {}",
                searchQuery, event.latitude(), event.longitude());

        UserQueryEvent searchEvent = new UserQueryEvent(
                sessionId,
                userId,
                searchQuery,
                event.ignoreAllergies(),
                event.isOpenNow(),
                event.latitude(),
                event.longitude(),
                event.radiusKm() != null ? event.radiusKm() : DEFAULT_RADIUS_KM
        );

        /*
        SearchResult searchResult = restaurantSearchService.findFreshPlacesV2(
                searchEvent, alreadyPresentedIds, intent.isNamedEntity()
        );

         */
        SearchResult searchResult = null;

        if (searchResult.documents().isEmpty()) {
            String notFoundMsg = "Üzgünüm, belirttiğiniz kriterlere uygun yakınlarda bir mekan bulamadım. Yarıçapı genişletmeyi deneyebilirsiniz.";
            fullResponse.append(notFoundMsg);
            notifyUser(userId, StreamEventType.TOKEN, notFoundMsg, false, 0);
            return Collections.emptyList();
        }

        List<PresentedPlaceSummary> places = searchResult.documents().stream()
                .map(metadataExtractor::fromDocument)
                .toList();

        log.info("[V2 Worker] {} mekan bulundu. Canlı Streaming başlatılıyor...", places.size());
        notifyUser(userId, StreamEventType.STATUS, "Mekanlar bulundu, öneriler hazırlanıyor... ✨", false, 0);

        String historyMd = buildChatHistoryMarkdown(fullHistory, DEFAULT_HISTORY_ROUNDS);
        String presentationPrompt = formatPlacesForPresentation(event.userQuery(), places, historyMd);

        ExecutionTokenUsage stage2Tokens = new ExecutionTokenUsage();

        Flux<ChatResponse> responseStream = client.prompt()
                .system(PRESENTATION_SYSTEM_PROMPT)
                .user(presentationPrompt)
                .stream()
                .chatResponse();

        responseStream
                .doOnNext(chatResponse -> {
                    String token = chatResponse.getResult().getOutput().getContent();
                    if (token != null && !token.isEmpty()) {
                        fullResponse.append(token);
                        notifyUser(userId, StreamEventType.TOKEN, token, false, 0);
                    }
                    if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
                        var usage = chatResponse.getMetadata().getUsage();
                        if (usage.getPromptTokens() != null && usage.getPromptTokens() > 0) {
                            stage2Tokens.inputTokens.set(usage.getPromptTokens());
                        }
                        if (usage.getGenerationTokens() != null && usage.getGenerationTokens() > 0) {
                            stage2Tokens.outputTokens.set(usage.getGenerationTokens());
                        }
                    }
                })
                .blockLast(STREAM_TIMEOUT);

        CostTracker.logSummary("2. Aşama (Streaming)", stage2Tokens.inputTokens.get(), stage2Tokens.outputTokens.get());

        long totalIn = intentTokens.inputTokens.get() + stage2Tokens.inputTokens.get();
        long totalOut = intentTokens.outputTokens.get() + stage2Tokens.outputTokens.get();
        CostTracker.logSummary("TOPLAM İSTEK MALİYETİ", totalIn, totalOut);

        return places;
    }

    private String formatPlacesForPresentation(
            String userQuery,
            List<PresentedPlaceSummary> places,
            String historyMarkdown
    ) {
        StringBuilder sb = new StringBuilder();

        if (!historyMarkdown.isBlank()) {
            sb.append(historyMarkdown).append("\n");
        }

        sb.append("GÜNCEL KULLANICI SORUSU: \"").append(userQuery).append("\"\n\n");
        sb.append("BULUNAN YENİ MEKANLAR:\n\n");

        for (PresentedPlaceSummary p : places) {
            sb.append(p.toMarkdown()).append("\n");
        }

        sb.append("\nTALİMAT: Geçmiş bağlamını dikkate alarak, YALNIZCA yukarıdaki mekanları samimi ve akıcı bir dille kullanıcıya tanıt.");
        return sb.toString();
    }

    private UserIntentAnalysis parseIntentResponse(String rawResponse, String fallbackQuery) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return new UserIntentAnalysis(true, false, fallbackQuery, "");
        }
        try {
            String cleanJson = rawResponse.replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();
            return objectMapper.readValue(cleanJson, UserIntentAnalysis.class);
        } catch (Exception e) {
            log.warn("Niyet JSON parse edilemedi, fallback arama yapılıyor: {}", e.getMessage());
            return new UserIntentAnalysis(true, false, fallbackQuery, "");
        }
    }


    private Set<String> extractAllPresentedIds(List<ChatHistoryMessage> history) {
        Set<String> placeIds = new HashSet<>();
        if (history == null) return placeIds;
        for (ChatHistoryMessage msg : history) {
            if (msg.role() == MessageRole.ASSISTANT && msg.metadata() != null) {
                Object placesObj = msg.metadata().get("presentedPlaces");
                if (placesObj != null) {
                    try {
                        List<PresentedPlaceSummary> list = objectMapper.convertValue(
                                placesObj, new TypeReference<List<PresentedPlaceSummary>>() {});
                        for (PresentedPlaceSummary p : list) {
                            if (p != null && p.qdrantId() != null) {
                                placeIds.add(p.qdrantId());
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        return placeIds;
    }

    private String buildChatHistoryMarkdown(List<ChatHistoryMessage> fullHistory, int lastChatMessageCount) {
        if (fullHistory == null || fullHistory.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("ÖNCEKİ SOHBET GEÇMİŞİ (Kısa Özet):\n");

        // Son 4 mesajı al (2 tur soru-cevap)
        int start = Math.max(0, fullHistory.size() - lastChatMessageCount);
        for (int i = start; i < fullHistory.size(); i++) {
            ChatHistoryMessage msg = fullHistory.get(i);
            String roleLabel = (msg.role() == MessageRole.USER) ? "Kullanıcı" : "Asistan";

            String cleanContent = msg.content() != null ? msg.content().trim() : "";

            // Asistanın uzun mekan tanıtımlarını kırp (Sadece ilk 120 karakteri al)
            if (msg.role() == MessageRole.ASSISTANT && cleanContent.length() > 120) {
                cleanContent = cleanContent.substring(0, 120).replaceAll("\\s+", " ") + "...";
            }

            sb.append(String.format("- **%s:** %s\n", roleLabel, cleanContent));
        }
        sb.append("\n");

        return sb.toString();
    }


    private void notifyUser(Long userId, StreamEventType type, String content, boolean hasMore, int remaining) {
        webSocketHandler.sendToUser(userId, new AiStreamPayload(userId, type, content, hasMore, remaining));
    }

    private static class ExecutionTokenUsage {
        final AtomicLong inputTokens = new AtomicLong(0);
        final AtomicLong outputTokens = new AtomicLong(0);
    }
}