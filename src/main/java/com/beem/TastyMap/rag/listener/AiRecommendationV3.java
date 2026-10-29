package com.beem.TastyMap.rag.listener;

import com.beem.TastyMap.rag.component.PlaceMetadataExtractor;
import com.beem.TastyMap.rag.data.document.RestaurantMetadata;
import com.beem.TastyMap.rag.data.dto.*;
import com.beem.TastyMap.rag.data.enums.MessageRole;
import com.beem.TastyMap.rag.data.event.UserQueryEvent;
import com.beem.TastyMap.rag.data.response.AiStreamPayload;
import com.beem.TastyMap.rag.data.response.AiStreamPayload.StreamEventType;
import com.beem.TastyMap.rag.factory.AiProviderFactory;
import com.beem.TastyMap.rag.service.ChatMemoryManager;
import com.beem.TastyMap.rag.service.RestaurantSearchService;
import com.beem.TastyMap.rag.service.UserDataCollectorService;
import com.beem.TastyMap.rag.util.CostTracker;
import com.beem.TastyMap.rag.websocket.AiStreamWebSocketHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
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
public class  AiRecommendationV3{

    private static final Logger log = LoggerFactory.getLogger(AiRecommendationV3.class);

    private static final Duration STREAM_TIMEOUT = Duration.ofSeconds(35);
    private static final int DEFAULT_HISTORY_ROUNDS = 2; // Son 2 soru-cevap (4 mesaj)
    private static final double DEFAULT_RADIUS_KM = 5.0;

    private final AiProviderFactory aiProviderFactory;
    private final ChatMemoryManager chatMemoryManager;
    private final AiStreamWebSocketHandler webSocketHandler;
    private final RestaurantSearchService restaurantSearchService;
    private final PlaceMetadataExtractor metadataExtractor;
    private final UserDataCollectorService userDataCollectorService;
    private final ObjectMapper objectMapper;

    private static final String INTENT_SYSTEM_PROMPT = """
        Sen TastyMap sisteminin niyet analiz motorusun.
        Kullanıcının mesajını ve varsa SAĞLIK / DİYET KISITLAMALARINI incele ve YALNIZCA geçerli bir JSON nesnesi formatında yanıt ver.
        Açıklama, markdown veya selamlama EKLEME.

        JSON ŞEMASI:
        - search_needed (boolean): Mekan araması gerekiyor mu?
        - is_named_entity (boolean): Belirli bir mekan/dükkan adı mı aranıyor?
        - semantic_query (string): Vektör tabanında aranacak temiz ve hedefe yönelik anahtar kelimeler
        - direct_reply (string): Arama gerekmiyorsa verilecek doğrudan samimi yanıt (arama varsa "")

        KRİTİK KURALLAR:
        1. Kullanıcı doğrudan belirli bir dükkan soruyorsa (örn: "Ay Işığı Kafe", "Döner House"):
           search_needed=true, is_named_entity=true, semantic_query="Mekan Adı", direct_reply=""
        2. SAĞLIK & DİYET ENJEKSİYONU:
           - Kullanıcı VEGAN ise ve genel yiyecek arıyorsa (örn: "hamburger", "pizza", "tatlı"), `semantic_query` alanına mutlaka vegan alternatifleri ekle (örn: "vegan burger bitkisel", "vegan pizza").
           - Kullanıcı VEJETARYEN ise et yemekleri yerine etsiz/vejetaryen anahtar kelimelerini semantic_query içine yerleştir.
           - Kullanıcının ÇÖLYAK / GLUTEN alerjisi varsa unlu/hamur işi sorgularında `semantic_query`'ye "glutensiz" ibaresini ekle.
           - Kullanıcının DİYABETİ varsa ve spesifik tatlı istemediyse tatlı araması üretme.
        3. Kullanıcı genel kategori/lezzet/ortam arıyorsa (örn: "sessiz kafe", "yakında dönerci", "ders çalışmalık yer"):
           search_needed=true, is_named_entity=false, semantic_query="aranacak kelimeler", direct_reply=""
        4. Kullanıcı sadece selam veriyor, hal hatır soruyor veya genel sohbet ediyorsa:
           search_needed=false, is_named_entity=false, semantic_query="", direct_reply="Samimi ve kısa cevabın"
        """;

    private static final String PRESENTATION_SYSTEM_PROMPT = """
        Sen TastyMap'in samimi ve uzman mekan rehberisin.

        KURALLAR:
        1. YALNIZCA sana verilen mekan listesindeki bilgileri (lezzet, ambiyans, dikkat edilecekler, mesafe) kullan. Listede olmayan mekan veya bilgi uydurma.
        2. Her mekanı akıcı ve net bir paragrafla tanıt. Mekan adını kalın (**Mekan Adı**) yaz.
        3. Mekanın öne çıkan lezzetlerini ve ortamını vurgula.
        4. KULLANICI SAĞLIK DUYARLILIĞI: Kullanıcının diyet türü (vegan, vejetaryen) veya alerjisi belirtilmişse, önerdiğin mekanların bu duruma uygun yönlerini mutlaka belirt. Çapraz bulaşma riski olabilecek alerjilerde (özellikle gluten, fıstık vb.) sipariş öncesi mekanla teyitleşmesini dostça tavsiye et.
        5. "Dikkat Edilmesi Gerekenler" kısmında olumsuz bir not varsa kullanıcıyı dostça uyar.
        6. Giriş ve kapanış cümlelerin samimi ve enerjik olsun.
        """;

    private static final String CONTEXT_QA_SYSTEM_PROMPT = """
        Sen TastyMap gastronomi asistanısın.
        Kullanıcı önerilen bir mekan hakkında detay soruyor.
        Sana verilen [MEKAN BİLGİSİ] alanındaki verileri (ortam, lezzetler, hizmet detayları, uyarılar) kullanarak soruya doğrudan ve samimi cevap ver.
        Bilmediğin bir detay varsa dürüstçe bilmediğini söyle, kesinlikle uydurma.
        """;

    public AiRecommendationV3(
            AiProviderFactory aiProviderFactory,
            ChatMemoryManager chatMemoryManager,
            AiStreamWebSocketHandler webSocketHandler,
            RestaurantSearchService restaurantSearchService,
            PlaceMetadataExtractor metadataExtractor,
            UserDataCollectorService userDataCollectorService,
            ObjectMapper objectMapper
    ) {
        this.aiProviderFactory = aiProviderFactory;
        this.chatMemoryManager = chatMemoryManager;
        this.webSocketHandler = webSocketHandler;
        this.restaurantSearchService = restaurantSearchService;
        this.metadataExtractor = metadataExtractor;
        this.userDataCollectorService = userDataCollectorService;
        this.objectMapper = objectMapper;
    }

    public void processAiRequest(UserQueryEvent event) {
        Long userId = event.userId();
        String sessionId = (event.sessionId() != null && !event.sessionId().isBlank())
                ? event.sessionId()
                : UUID.randomUUID().toString();

        long startTime = System.currentTimeMillis();
        log.info("[V3 Worker] RAG İsteği Başladı -> User: {}, Session: {}, Query: '{}'", userId, sessionId, event.userQuery());

        notifyUser(userId, StreamEventType.STATUS, "İsteğiniz inceleniyor... 🔍", false, 0);

        UserDataCollectorService.UserContextData userContext = userDataCollectorService.collectUserData(userId);
        UserHealthProfile healthProfile = userContext.healthProfile();

        List<ChatHistoryMessage> fullHistory = chatMemoryManager.getRecentMessages(sessionId);
        recordUserMessage(sessionId, userId, event);

        List<RestaurantMetadata> activePlaces = chatMemoryManager.extractLastPresentedPlaces(fullHistory);
        Set<String> allPresentedIds = chatMemoryManager.extractAllPresentedIds(fullHistory);

        List<RestaurantMetadata> newlyFoundPlaces = new ArrayList<>();
        StringBuilder fullResponse = new StringBuilder();

        try {
            ChatClient client = aiProviderFactory.getDefaultClient();

            // 1. Aşama: Niyet Analizi
            ExecutionTokenUsage intentTokens = new ExecutionTokenUsage();
            UserIntentAnalysis intent = analyzeUserIntent(client, event.userQuery(), healthProfile, activePlaces, intentTokens);

            switch (intent.action()) {
                case CONTEXT_QA -> {
                    log.info("🧠 [Router] CONTEXT_QA tetiklendi. Hedef Mekan: '{}'", intent.targetPlaceName());
                    handleContextQa(client, userId, event.userQuery(), intent.targetPlaceName(), activePlaces, fullResponse, intentTokens);
                }
                case CHITCHAT -> {
                    log.info("💬 [Router] CHITCHAT tetiklendi.");
                    handleDirectReply(userId, intent, fullResponse);
                }
                case FETCH_MORE -> {
                    log.info("🔄 [Router] FETCH_MORE tetiklendi. Önceki mekanlar dışlanıyor.");
                    newlyFoundPlaces = executeSearchAndStream(
                            client, event, sessionId, userId, intent.semanticQuery(), false, healthProfile,
                            allPresentedIds, fullResponse, intentTokens
                    );
                }
                case NAMED_ENTITY_SEARCH -> {
                    log.info("🎯 [Router] NAMED_ENTITY_SEARCH tetiklendi: '{}'", intent.targetPlaceName());
                    newlyFoundPlaces = executeSearchAndStream(
                            client, event, sessionId, userId, intent.semanticQuery(), true, healthProfile,
                            Collections.emptySet(), fullResponse, intentTokens
                    );
                }
                case NEW_SEARCH -> {
                    log.info("🔍 [Router] NEW_SEARCH tetiklendi: '{}'", intent.semanticQuery());
                    newlyFoundPlaces = executeSearchAndStream(
                            client, event, sessionId, userId, intent.semanticQuery(), false, healthProfile,
                            Collections.emptySet(), fullResponse, intentTokens
                    );
                }
            }

            notifyUser(userId, StreamEventType.COMPLETED, "", false, 0);
            chatMemoryManager.recordAssistantMessage(sessionId, userId, fullResponse.toString(), newlyFoundPlaces);

            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("[V3 Worker] İşlem Tamamlandı. Toplam Süre: {} ms, Sunulan: {}", totalDuration, newlyFoundPlaces.size());

        } catch (Exception e) {
            log.error("[V3 Worker] Kritik Hata: {}", e.getMessage(), e);
            notifyUser(userId, StreamEventType.ERROR, "Tavsiyeler hazırlanırken bir sorun oluştu.", false, 0);
        }
    }

    private UserIntentAnalysis analyzeUserIntent(
            ChatClient client,
            String userQuery,
            UserHealthProfile healthProfile,
            List<RestaurantMetadata> activePlaces,
            ExecutionTokenUsage tokenUsage
    ) {
        log.info("[V3 Worker] 1. Aşama: Niyet ve arama analizi yapılıyor...");

        StringBuilder userPromptBuilder = new StringBuilder();

        if (activePlaces != null && !activePlaces.isEmpty()) {
            userPromptBuilder.append("[AKTİF BAĞLAMDAKİ MEKANLAR (Kullanıcının ekranında açık olanlar)]:\n");
            for (RestaurantMetadata m: activePlaces) {
                userPromptBuilder.append(
                        String.format("- %s (Kategori: %s)\n", m.getName(), m.getVenueCategory())
                );
            }
        }

        userPromptBuilder.append("Kullanıcı İletisi: ").append(userQuery).append("\n");

        if (healthProfile != null && healthProfile.isRestricted()) {
            userPromptBuilder.append("Kullanıcı Sağlık/Diyet Profili: ").append(healthProfile).append("\n");
        }

        Prompt intentPrompt = new Prompt(List.of(
                new SystemMessage(INTENT_SYSTEM_PROMPT),
                new UserMessage(userPromptBuilder.toString())
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

        log.info("[V3 Worker] Niyet Çıktısı: {}", rawOutput);
        return parseIntentResponse(rawOutput, userQuery);
    }

    private List<PresentedPlaceSummary> executeSearchAndStream(
            ChatClient client,
            UserQueryEvent event,
            String sessionId,
            Long userId,
            UserIntentAnalysis intent,
            UserHealthProfile healthProfile,
            Set<String> alreadyPresentedIds,
            List<ChatHistoryMessage> fullHistory,
            StringBuilder fullResponse,
            ExecutionTokenUsage intentTokens
    ) {
        String searchQuery = (intent.semanticQuery() != null && !intent.semanticQuery().isBlank())
                ? intent.semanticQuery()
                : event.userQuery();

        log.info("🔍 [V3 Worker] Qdrant Arama Tetikleniyor -> Query: '{}', Lat: {}, Lon: {}",
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

        SearchResult searchResult = restaurantSearchService.findFreshPlacesV2(
                searchEvent, alreadyPresentedIds, intent.isNamedEntity(), healthProfile
        );

        if (searchResult.documents() == null || searchResult.documents().isEmpty()) {
            String notFoundMsg = "Üzgünüm, belirttiğiniz kriterlere uygun yakınlarda bir mekan bulamadım. Yarıçapı genişletmeyi deneyebilirsiniz.";
            fullResponse.append(notFoundMsg);
            notifyUser(userId, StreamEventType.TOKEN, notFoundMsg, false, 0);
            return Collections.emptyList();
        }

        List<PresentedPlaceSummary> places = searchResult.documents().stream()
                .map(metadataExtractor::fromDocument)
                .toList();

        log.info("[V3 Worker] {} mekan bulundu. Canlı Streaming başlatılıyor...", places.size());
        notifyUser(userId, StreamEventType.STATUS, "Mekanlar bulundu, öneriler hazırlanıyor... ✨", false, 0);

        String historyMd = buildChatHistoryMarkdown(fullHistory, DEFAULT_HISTORY_ROUNDS);
        String presentationPrompt = formatPlacesForPresentation(event.userQuery(), places, historyMd, healthProfile);

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

    private void streamToUser(Flux<ChatResponse> stream, Long userId, StringBuilder fullResponse, ExecutionTokenUsage tokenUsage) {
        stream.doOnNext(chatResponse -> {
            String token = chatResponse.getResult().getOutput().getContent();
            if (token != null && !token.isEmpty()) {
                fullResponse.append(token);
                notifyUser(userId, StreamEventType.TOKEN, token, false, 0);
            }
            if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
                var usage = chatResponse.getMetadata().getUsage();
                if (usage.getPromptTokens() != null && usage.getPromptTokens() > 0) {
                    tokenUsage.inputTokens.set(usage.getPromptTokens());
                }
                if (usage.getGenerationTokens() != null && usage.getGenerationTokens() > 0) {
                    tokenUsage.outputTokens.set(usage.getGenerationTokens());
                }
            }
        }).blockLast(STREAM_TIMEOUT);
    }

    private void handleContextQa(
            ChatClient client,
            Long userId,
            String userQuery,
            String targetPlaceName,
            List<RestaurantMetadata> activePlaces,
            StringBuilder fullResponse,
            ExecutionTokenUsage intentTokens
    ) {
        if (activePlaces == null || activePlaces.isEmpty()) {
            String fallback = "Bahsettiğiniz mekanı sohbet geçmişimde bulamadım, tekrar aratmamı ister misiniz?";
            fullResponse.append(fallback);
            notifyUser(userId, StreamEventType.TOKEN, fallback, false, 0);
            return;
        }

        String searchName = (targetPlaceName != null) ? targetPlaceName.toLowerCase(Locale.ROOT) : "";
        RestaurantMetadata targetPlace = activePlaces.stream()
                .filter(p -> p.getName() != null && (p.getName().toLowerCase(Locale.ROOT).contains(searchName)
                        || searchName.contains(p.getName().toLowerCase(Locale.ROOT))))
                .findFirst()
                .orElse(activePlaces.get(0));

        String promptContent = String.format("""
                Kullanıcı Sorusu: "%s"

                [MEKAN BİLGİSİ]:
                %s
                """, userQuery, targetPlace.toPresentationMarkdown());

        ExecutionTokenUsage qaTokens = new ExecutionTokenUsage();

        Flux<ChatResponse> responseStream = client.prompt()
                .system(CONTEXT_QA_SYSTEM_PROMPT)
                .user(promptContent)
                .stream()
                .chatResponse();

        streamToUser(responseStream, userId, fullResponse, qaTokens);

        CostTracker.logSummary("2. Aşama (Context QA Streaming)", qaTokens.inputTokens.get(), qaTokens.outputTokens.get());
        CostTracker.logSummary("TOPLAM İSTEK MALİYETİ", intentTokens.inputTokens.get() + qaTokens.inputTokens.get(),
                intentTokens.outputTokens.get() + qaTokens.outputTokens.get());
    }

    private void handleDirectReply(Long userId, UserIntentAnalysis intent, StringBuilder fullResponse) {
        String reply = (intent.directReply() != null && !intent.directReply().isBlank())
                ? intent.directReply()
                : "Merhaba! Size en lezzetli mekanları bulmak için buradayım. Ne arzu edersiniz?";
        fullResponse.append(reply);
        notifyUser(userId, StreamEventType.TOKEN, reply, false, 0);
    }

    private String formatPlacesForPresentation(
            String userQuery,
            List<PresentedPlaceSummary> places,
            String historyMarkdown,
            UserHealthProfile healthProfile
    ) {
        StringBuilder sb = new StringBuilder();

        if (!historyMarkdown.isBlank()) {
            sb.append(historyMarkdown).append("\n");
        }

        sb.append("GÜNCEL KULLANICI SORUSU: \"").append(userQuery).append("\"\n\n");

        if (healthProfile != null && healthProfile.isRestricted()) {
            sb.append("DİKKAT - KULLANICI SAĞLIK PROFİLİ: ").append(healthProfile).append("\n");
        }
        sb.append("\n");

        sb.append("BULUNAN YENİ MEKANLAR:\n\n");

        for (PresentedPlaceSummary p : places) {
            sb.append(p.toMarkdown()).append("\n");
        }
        String healthSentence = healthProfile != null && healthProfile.isRestricted()
                ? "ve varsa kullanıcının sağlık kısıtlamalarını " : "";

        sb.append("\nTALİMAT: Geçmiş bağlamını ").append(healthSentence).append("dikkate alarak, YALNIZCA yukarıdaki mekanları samimi ve akıcı bir dille kullanıcıya tanıt.");
        return sb.toString();
    }

    private String buildChatHistoryMarkdown(List<ChatHistoryMessage> fullHistory, int lastRounds) {
        if (fullHistory == null || fullHistory.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("ÖNCEKİ SOHBET GEÇMİŞİ (Kısa Özet):\n");

        int messageCount = lastRounds * 2;
        int start = Math.max(0, fullHistory.size() - messageCount);

        for (int i = start; i < fullHistory.size(); i++) {
            ChatHistoryMessage msg = fullHistory.get(i);
            String roleLabel = (msg.role() == MessageRole.USER) ? "Kullanıcı" : "Asistan";
            String cleanContent = msg.content() != null ? msg.content().trim() : "";

            if (msg.role() == MessageRole.ASSISTANT && cleanContent.length() > 120) {
                cleanContent = cleanContent.substring(0, 120).replaceAll("\\s+", " ") + "...";
            }

            sb.append(String.format("- **%s:** %s\n", roleLabel, cleanContent));
        }
        sb.append("\n");
        return sb.toString();
    }


    private UserIntentAnalysis parseIntentResponse(String rawResponse, String fallbackQuery) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return UserIntentAnalysis.fallback(fallbackQuery);
        }
        try {
            String cleanJson = rawResponse.replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();
            return objectMapper.readValue(cleanJson, UserIntentAnalysis.class);
        } catch (Exception e) {
            log.warn("Niyet JSON parse edilemedi, fallback yapılıyor: {}", e.getMessage());
            return UserIntentAnalysis.fallback(fallbackQuery);
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


    private void notifyUser(Long userId, StreamEventType type, String content, boolean hasMore, int remaining) {
        webSocketHandler.sendToUser(userId, new AiStreamPayload(userId, type, content, hasMore, remaining));
    }

    private static class ExecutionTokenUsage {
        final AtomicLong inputTokens = new AtomicLong(0);
        final AtomicLong outputTokens = new AtomicLong(0);
    }
}