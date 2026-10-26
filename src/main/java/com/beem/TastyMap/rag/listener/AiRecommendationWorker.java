package com.beem.TastyMap.rag.listener;

import com.beem.TastyMap.rag.component.AiPromptProvider;
import com.beem.TastyMap.rag.component.PlaceMetadataExtractor;
import com.beem.TastyMap.rag.config.KafkaConfig;
import com.beem.TastyMap.rag.data.dto.ChatHistoryMessage;
import com.beem.TastyMap.rag.data.dto.PresentedPlaceSummary;
import com.beem.TastyMap.rag.data.dto.SearchResult;
import com.beem.TastyMap.rag.data.enums.MessageRole;
import com.beem.TastyMap.rag.data.event.UserQueryEvent;
import com.beem.TastyMap.rag.data.response.AiStreamPayload;
import com.beem.TastyMap.rag.data.response.AiStreamPayload.StreamEventType;
import com.beem.TastyMap.rag.service.ChatMemoryManager;
import com.beem.TastyMap.rag.service.RestaurantSearchService;
import com.beem.TastyMap.rag.tool.RestaurantSearchTool;
import com.beem.TastyMap.rag.websocket.AiStreamWebSocketHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.*;

@Component
public class AiRecommendationWorker {

    private static final Logger log = LoggerFactory.getLogger(AiRecommendationWorker.class);

    private final ChatClient.Builder chatClientBuilder;
    private final ChatMemoryManager chatMemoryManager;
    private final AiStreamWebSocketHandler webSocketHandler;
    private final RestaurantSearchService restaurantSearchService;
    private final PlaceMetadataExtractor metadataExtractor;
    private final AiPromptProvider promptProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiRecommendationWorker(
            ChatClient.Builder chatClientBuilder,
            ChatMemoryManager chatMemoryManager,
            AiStreamWebSocketHandler webSocketHandler,
            RestaurantSearchService restaurantSearchService,
            PlaceMetadataExtractor metadataExtractor,
            AiPromptProvider promptProvider
    ) {
        this.chatClientBuilder = chatClientBuilder;
        this.chatMemoryManager = chatMemoryManager;
        this.webSocketHandler = webSocketHandler;
        this.restaurantSearchService = restaurantSearchService;
        this.metadataExtractor = metadataExtractor;
        this.promptProvider = promptProvider;
    }

    @KafkaListener(topics = KafkaConfig.RESTAURANT_AI_REQUESTS_TOPIC, groupId = "tasty-ai-group")
    public void processAiRequest(UserQueryEvent event) {
        Long userId = event.userId();
        String sessionId = (event.sessionId() != null && !event.sessionId().isBlank())
                ? event.sessionId()
                : UUID.randomUUID().toString();

        log.info("RAG İsteği -> User: {}, Session: {}, Query: '{}'", userId, sessionId, event.userQuery());

        notifyUser(userId, StreamEventType.STATUS, "İsteğiniz inceleniyor... 🔍", false, 0);

        // 1. Geçmişi çek ve kullanıcının mesajını kaydet
        List<ChatHistoryMessage> fullHistory = chatMemoryManager.getRecentMessages(sessionId);
        recordUserMessage(sessionId, userId, event);

        // 2. Bu sohbette adı geçen tüm ID'leri topla
        Set<String> alreadyPresentedIds = extractAllPresentedIds(fullHistory);

        // Modelin bulduğu mekanları tutacak thread-safe liste (ToolSessionContext yerine doğrudan lokal değişken)
        List<PresentedPlaceSummary> newlyFoundPlaces = Collections.synchronizedList(new ArrayList<>());

        try {
            String systemRules = promptProvider.getSystemRules();

            List<Message> historyMessages = convertToTrimmedMessages(fullHistory);
            ChatClient client = chatClientBuilder.build();

            // =========================================================================
            // AŞAMA 1: MODELİN KARAR VE ARAÇ TETİKLEME AŞAMASI (Senkron / Hızlı)
            // =========================================================================
            log.info("1. Aşama: Model niyet kontrolü ve araç yürütme başlatılıyor...");

            ChatResponse executionResponse = client.prompt()
                    .system(systemRules)
                    .messages(historyMessages)
                    .user(event.userQuery())
                    .function(
                            "searchRestaurantsTool",
                            "Kullanıcı herhangi bir kafe, kahveci, restoran, yemek veya mekan sorduğunda veritabanını sorgulamak için KULLANILMASI ZORUNLU OLAN TEMEL ARAÇTIR. Kullanıcı kahve, tatlı, yemek veya mekan tavsiyesi istediğinde bu aracı MUTLAKA çağır.",
                            RestaurantSearchTool.Request.class, // <-- TİPİ BURADA AÇIKÇA BELİRTİYORUZ (ClassCastException'ı ÇÖZER!)
                            request -> {
                                log.info("🤖 MODEL TOOL ÇAĞIRDI -> Query: '{}', User Lat: {}, Lon: {}",
                                        request.searchQuery(), event.latitude(), event.longitude());

                                UserQueryEvent searchEvent = new UserQueryEvent(
                                        sessionId,
                                        userId,
                                        request.searchQuery(),
                                        event.ignoreAllergies(),
                                        event.isOpenNow(),
                                        event.latitude(),
                                        event.longitude(),
                                        event.radiusKm() != null ? event.radiusKm() : 10.0
                                );

                                SearchResult result = null;// = restaurantSearchService.findFreshPlacesV2(searchEvent, alreadyPresentedIds);

                                if (result.documents().isEmpty()) {
                                    return new RestaurantSearchTool.Response(Collections.emptyList(), 0, "Bu mesafede uygun mekan bulunamadı.");
                                }

                                List<PresentedPlaceSummary> summaries = result.documents().stream()
                                        .map(metadataExtractor::fromDocument)
                                        .toList();

                                newlyFoundPlaces.addAll(summaries);
                                return new RestaurantSearchTool.Response(summaries, result.remainingCount(), "Mekanlar bulundu.");
                            }
                    )
                    .call()
                    .chatResponse();

            String intermediateContent = executionResponse.getResult().getOutput().getContent();
            boolean toolWasExecuted = !newlyFoundPlaces.isEmpty();

            StringBuilder fullResponse = new StringBuilder();

            // =========================================================================
            // AŞAMA 2: KULLANICIYA CANLI STREAMING İLE YANIT AKITMA
            // =========================================================================
            if (toolWasExecuted) {
                log.info("2. Aşama: {} adet yeni mekan bulundu, streaming başlatılıyor...", newlyFoundPlaces.size());
                notifyUser(userId, StreamEventType.STATUS, "Mekanlar bulundu, öneriler hazırlanıyor... ✨", false, 0);

                String promptWithToolData = formatPlacesForFinalStreaming(event.userQuery(), newlyFoundPlaces);

                Flux<String> tokenStream = client.prompt()
                        .system(systemRules)
                        .messages(historyMessages)
                        .user(promptWithToolData)
                        .stream()
                        .content();

                tokenStream
                        .doOnNext(token -> {
                            fullResponse.append(token);
                            notifyUser(userId, StreamEventType.TOKEN, token, false, 0);
                        })
                        .blockLast(Duration.ofSeconds(360));

            } else {
                log.info("Model araç çağırmadı, doğrudan mevcut bağlamdan cevap verildi.");
                if (intermediateContent != null) {
                    fullResponse.append(intermediateContent);
                    notifyUser(userId, StreamEventType.TOKEN, intermediateContent, false, 0);
                }
            }

            notifyUser(userId, StreamEventType.COMPLETED, "", false, 0);

            // 5. Metadata Kaydı
            Map<String, Object> assistantMeta = new HashMap<>();
            if (toolWasExecuted) {
                assistantMeta.put("presentedPlaces", new ArrayList<>(newlyFoundPlaces));
            }

            chatMemoryManager.recordMessage(sessionId, userId, MessageRole.ASSISTANT, fullResponse.toString(), assistantMeta);

        } catch (Exception e) {
            log.error("İşlem sırasında hata: {}", e.getMessage(), e);
            notifyUser(userId, StreamEventType.ERROR, "Yanıt üretilirken bir hata oluştu.", false, 0);
        }
    }

    private String formatPlacesForFinalStreaming(String userQuery, List<PresentedPlaceSummary> places) {
        StringBuilder sb = new StringBuilder("KULLANICI İSTEĞİ: \"").append(userQuery).append("\"\n\n");
        sb.append("AŞAĞIDAKİ MEKANLAR KONYA'DADIR VE BULUNMUŞTUR:\n");
        for (PresentedPlaceSummary p : places) {
            String safeAddress = (p.address() != null && !p.address().isBlank()) ? p.address() : "Konya";
            sb.append(String.format("- **%s** | Adres: %s | Puan: %.1f ⭐\n  Yorum Özeti: %s\n",
                    p.name(), safeAddress, p.googleRating() != null ? p.googleRating() : 0.0, p.highlightReviews()));
        }
        sb.append("\nTALİMAT: Yukarıdaki mekanları incele ve kullanıcının isteğine uygun olanları şablona sadık kalarak samimi bir dille tanıt.");
        return sb.toString();
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

    private List<Message> convertToTrimmedMessages(List<ChatHistoryMessage> fullHistory) {
        List<Message> messages = new ArrayList<>();
        if (fullHistory == null || fullHistory.isEmpty()) return messages;
        int start = Math.max(0, fullHistory.size() - 4);
        for (int i = start; i < fullHistory.size(); i++) {
            ChatHistoryMessage msg = fullHistory.get(i);
            if (msg.role() == MessageRole.USER) {
                messages.add(new UserMessage(msg.content()));
            } else if (msg.role() == MessageRole.ASSISTANT) {
                messages.add(new AssistantMessage(msg.content()));
            }
        }
        return messages;
    }

    private void recordUserMessage(String sessionId, Long userId, UserQueryEvent event) {
        Map<String, Object> userMeta = new HashMap<>();
        if (event.latitude() != null && event.longitude() != null) {
            userMeta.put("lat", event.latitude());
            userMeta.put("lon", event.longitude());
        }
        chatMemoryManager.recordMessage(sessionId, userId, MessageRole.USER, event.userQuery(), userMeta);
    }

    private void notifyUser(Long userId, StreamEventType type, String content, boolean hasMore, int remaining) {
        webSocketHandler.sendToUser(userId, new AiStreamPayload(userId, type, content, hasMore, remaining));
    }
}