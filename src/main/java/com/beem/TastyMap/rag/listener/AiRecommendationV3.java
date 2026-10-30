package com.beem.TastyMap.rag.listener;

import com.beem.TastyMap.rag.data.document.RestaurantMetadata;
import com.beem.TastyMap.rag.data.dto.*;
import com.beem.TastyMap.rag.data.enums.AiProvider;
import com.beem.TastyMap.rag.data.enums.MessageRole;
import com.beem.TastyMap.rag.data.enums.UserIntentAction;
import com.beem.TastyMap.rag.data.event.UserQueryEvent;
import com.beem.TastyMap.rag.data.response.AiStreamPayload;
import com.beem.TastyMap.rag.data.response.AiStreamPayload.StreamEventType;
import com.beem.TastyMap.rag.entity.ChatMessageEntity;
import com.beem.TastyMap.rag.factory.AiProviderFactory;
import com.beem.TastyMap.rag.service.AiQuotaService;
import com.beem.TastyMap.rag.service.ChatMemoryManager;
import com.beem.TastyMap.rag.service.RestaurantSearchService;
import com.beem.TastyMap.rag.service.UserDataCollectorService;
import com.beem.TastyMap.rag.util.CostTracker;
import com.beem.TastyMap.rag.websocket.AiStreamWebSocketHandler;
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
import java.util.concurrent.atomic.AtomicReference;

@Component
public class  AiRecommendationV3{

    private static final Logger log = LoggerFactory.getLogger(AiRecommendationV3.class);

    private static final Duration STREAM_TIMEOUT = Duration.ofSeconds(35);
    private static final int DEFAULT_HISTORY_ROUNDS = 2; // Son 2 soru-cevap (4 mesaj)
    private static final double DEFAULT_RADIUS_KM = 5.0;
    private static final Locale TR = Locale.forLanguageTag("tr");

    private final AiProviderFactory aiProviderFactory;
    private final AiQuotaService aiQuotaService;
    private final ChatMemoryManager chatMemoryManager;
    private final AiStreamWebSocketHandler webSocketHandler;
    private final RestaurantSearchService restaurantSearchService;
    private final UserDataCollectorService userDataCollectorService;
    private final ObjectMapper objectMapper;

    private static final String INTENT_SYSTEM_PROMPT = """
        Sen TastyMap sisteminin Niyet Analiz Motorusun.
        Kullanıcının mesajını, aktif bağlamdaki mekanları ve sağlık kısıtlarını inceleyip YALNIZCA geçerli bir JSON nesnesi dön. Açıklama veya markdown ekleme.

        JSON ŞEMASI:
        {
          "action": "CONTEXT_QA | NAMED_ENTITY_SEARCH | NEW_SEARCH | FETCH_MORE | CHITCHAT",
          "target_place_name": "sorulan mekan adı (varsa, yoksa \"\")",
          "semantic_query": "vektör araması için hedefe yönelik temiz kelimeler (yoksa \"\")",
          "direct_reply": "sadece CHITCHAT ise samimi kısa cevap, yoksa \"\""
        }

        AKSIYON KURALLARI:
        1. CONTEXT_QA: Kullanıcı ekranda açık olan [AKTİF BAĞLAMDAKİ MEKANLAR] listesindeki bir yer hakkında detay soruyorsa (priz, saat, otopark, ortam vb.).
        2. NAMED_ENTITY_SEARCH: Kullanıcı ekranda olmayan belirli bir işletmeyi arıyorsa (örn: 'Karahan Döner').
        3. FETCH_MORE: Kullanıcı alternatif veya başka öneri istiyorsa (örn: 'başka ne var', 'farklı yer').
        4. NEW_SEARCH: Kullanıcı genel yemek/kategori arıyorsa (örn: 'sessiz kafe', 'güzel kebapçı').
        5. CHITCHAT: Selamlama, teşekkür veya genel sohbet.

        SORGU ZENGİNLEŞTİRME KURALI:
        NEW_SEARCH durumunda kullanıcının bir diyet/sağlık kısıtı varsa (Vegan, Çölyak vb.), `semantic_query` alanına bu kısıtı mutlaka anahtar kelime olarak ekle (Örn: 'mantı' -> 'glutensiz mantı').
        """;

    private static final String PRESENTATION_SYSTEM_PROMPT = """
    Sen TastyMap'in samimi, enerjik ve uzman gastronomi rehberisin.

    TEMEL KURALLAR:
    1. YALNIZCA sana verilen mekan bilgilerini kullan. Asla listede olmayan mekan veya özellik uydurma.
    2. Her mekanı akıcı bir paragrafla tanıt. Mekan adını mutlaka kalın (**Mekan Adı**) yaz.
    3. Mekanın öne çıkan lezzetlerini ve ortamını vurgula.
    4. SAĞLIK VE DİYET:
       - Promptta 'KULLANICI SAĞLIK PROFİLİ' başlığı VARSA bunu hayati kabul et; mekanları bu kısıtlara göre titizlikle değerlendir ve gerekirse uyar.
       - Bu başlık YOKSA alerji, kısıt veya çapraz bulaşma konusuna KESİNLİKLE değinme.
    5. 'Dikkat Edilmesi Gerekenler' alanında olumsuz bir not varsa kullanıcıyı samimi bir dille uyar; yoksa olumsuzluk uydurma.
    6. Giriş ve kapanış cümlelerin samimi ve davetkar olsun.
    """;

    private static final String CONTEXT_QA_SYSTEM_PROMPT = """
        Sen TastyMap gastronomi asistanısın.
        Kullanıcı önerilen bir mekan hakkında spesifik bir soru soruyor.

        KURALLAR:
        1. Sana verilen [MEKAN BİLGİSİ] içindeki ortam, lezzet, hizmet detayları ve uyarıları temel alarak soruya doğrudan ve samimi cevap ver.
        2. Bilgi verilen metinde açıkça yer almıyorsa "Bu konuda net bir bilgi bulunmuyor, mekana gitmeden önce teyit etmenizi öneririm" de. Asla bilgi uydurma.
        3. Cevabın gereksiz yere uzun olmasın; doğrudan kullanıcının merak ettiği konuya odaklan.
        """;

    public AiRecommendationV3(
            AiProviderFactory aiProviderFactory,
            AiQuotaService aiQuotaService,
            ChatMemoryManager chatMemoryManager,
            AiStreamWebSocketHandler webSocketHandler,
            RestaurantSearchService restaurantSearchService,
            UserDataCollectorService userDataCollectorService,
            ObjectMapper objectMapper
    ) {
        this.aiProviderFactory = aiProviderFactory;
        this.aiQuotaService = aiQuotaService;
        this.chatMemoryManager = chatMemoryManager;
        this.webSocketHandler = webSocketHandler;
        this.restaurantSearchService = restaurantSearchService;
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

        if (!aiQuotaService.hasAvailableQuota(userId)) {
            log.warn("⛔ [V3 Worker] Kullanıcı {} günlük AI kotasını doldurdu. İstek reddedildi.", userId);
            notifyUser(userId, StreamEventType.ERROR, "Günlük AI arama kotanız doldu. Lütfen yarın tekrar deneyin.", false, 0);
            return;
        }

        notifyUser(userId, StreamEventType.STATUS, "İsteğiniz inceleniyor... 🔍", false, 0);

        UserDataCollectorService.UserContextData userContext = userDataCollectorService.collectUserData(userId);
        UserHealthProfile healthProfile = userContext.healthProfile();

        List<ChatHistoryMessage> fullHistory = chatMemoryManager.getRecentMessages(sessionId);
        recordUserMessage(sessionId, userId, event);

        List<RestaurantMetadata> activePlaces = chatMemoryManager.extractLastPresentedPlaces(fullHistory);
        Set<String> allPresentedIds = chatMemoryManager.extractAllPresentedIds(fullHistory);

        List<RestaurantMetadata> newlyFoundPlaces = new ArrayList<>();
        StringBuilder fullResponse = new StringBuilder();

        AiProvider selectedProvider = (event.provider() != null && !event.provider().isBlank())
                ? AiProvider.fromString(event.provider())
                : aiProviderFactory.getDefaultProvider();
        ChatClient client = aiProviderFactory.getClient(selectedProvider);

        AtomicReference<String> activeModelName = new AtomicReference<>(selectedProvider.name().toLowerCase());
        ExecutionTokenUsage intentTokens = new ExecutionTokenUsage();
        ExecutionTokenUsage stage2Tokens = new ExecutionTokenUsage();

        try {
            UserIntentAnalysis intent = analyzeUserIntent(client, event.userQuery(), healthProfile, activePlaces, intentTokens, activeModelName);

            switch (intent.action()) {
                case CONTEXT_QA -> {
                    log.info("🧠 [Router] CONTEXT_QA tetiklendi. Hedef Mekan: '{}'", intent.targetPlaceName());
                    handleContextQa(client, userId, event.userQuery(), intent.targetPlaceName(), activePlaces, fullResponse, stage2Tokens, activeModelName);
                    newlyFoundPlaces = activePlaces;
                }
                case CHITCHAT -> {
                    log.info("💬 [Router] CHITCHAT tetiklendi.");
                    handleDirectReply(userId, intent, fullResponse);
                }
                case FETCH_MORE -> {
                    log.info("🔄 [Router] FETCH_MORE tetiklendi. Önceki mekanlar dışlanıyor.");
                    newlyFoundPlaces = executeSearchAndStream(
                            client, event, sessionId, userId, intent.semanticQuery(), false, healthProfile,
                            allPresentedIds, fullResponse, stage2Tokens, activeModelName
                    );
                }
                case NAMED_ENTITY_SEARCH -> {
                    log.info("🎯 [Router] NAMED_ENTITY_SEARCH tetiklendi: '{}'", intent.targetPlaceName());
                    newlyFoundPlaces = executeSearchAndStream(
                            client, event, sessionId, userId, intent.semanticQuery(), true, healthProfile,
                            Collections.emptySet(), fullResponse, stage2Tokens, activeModelName
                    );
                }
                case NEW_SEARCH -> {
                    log.info("🔍 [Router] NEW_SEARCH tetiklendi: '{}'", intent.semanticQuery());
                    newlyFoundPlaces = executeSearchAndStream(
                            client, event, sessionId, userId, intent.semanticQuery(), false, healthProfile,
                            Collections.emptySet(), fullResponse, stage2Tokens, activeModelName
                    );
                }
            }

            notifyUser(userId, StreamEventType.COMPLETED, "", false, 0);
            ChatMessageEntity savedAssistantMessage = chatMemoryManager.recordAssistantMessage(
                    sessionId, userId, fullResponse.toString(), newlyFoundPlaces
            );
            Long messageId = (savedAssistantMessage != null) ? savedAssistantMessage.getId() : null;

            if (intent.action() != UserIntentAction.CHITCHAT && !fullResponse.isEmpty()) {
                if (stage2Tokens.outputTokens.get() == 0) {
                    int estimatedOutput = Math.max(1, fullResponse.length() / 4);
                    stage2Tokens.outputTokens.set(estimatedOutput);
                    log.warn("⚠️ [Cost Tracker] 2. Aşama için Usage dönmedi, çıktı tahminlendi: {} token", estimatedOutput);
                }

                if (stage2Tokens.inputTokens.get() == 0) {
                    int estimatedInput = 250 + (newlyFoundPlaces.size() * 150);
                    stage2Tokens.inputTokens.set(estimatedInput);
                    log.warn("⚠️ [Cost Tracker] 2. Aşama girdi token'ı tahminlendi: {} token", estimatedInput);
                }
            }

            long totalDuration = System.currentTimeMillis() - startTime;
            int totalPromptTokens = (int) (intentTokens.inputTokens.get() + stage2Tokens.inputTokens.get());
            int totalCompletionTokens = (int) (intentTokens.outputTokens.get() + stage2Tokens.outputTokens.get());

            CostTracker.logSummary("TOPLAM İSTEK MALİYETİ", totalPromptTokens, totalCompletionTokens);

            aiQuotaService.recordUsageAndDeductQuota(
                    userId,
                    sessionId,
                    messageId,
                    selectedProvider,
                    activeModelName.get(),
                    totalPromptTokens,
                    totalCompletionTokens,
                    totalDuration
            );

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
            ExecutionTokenUsage tokenUsage,
            AtomicReference<String> activeModelName
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

        if (response != null && response.getMetadata() != null) {
            if (response.getMetadata().getUsage() != null) {
                var usage = response.getMetadata().getUsage();
                tokenUsage.inputTokens.set(usage.getPromptTokens() != null ? usage.getPromptTokens() : 0);
                tokenUsage.outputTokens.set(usage.getGenerationTokens() != null ? usage.getGenerationTokens() : 0);
                CostTracker.logSummary("1. Aşama (Niyet Analizi)", tokenUsage.inputTokens.get(), tokenUsage.outputTokens.get());
            }
            if (response.getMetadata().getModel() != null) {
                activeModelName.set(response.getMetadata().getModel());
            }
        }

        log.info("[V3 Worker] Niyet Çıktısı: {}", rawOutput);
        return parseIntentResponse(rawOutput, userQuery);
    }

    private List<RestaurantMetadata> executeSearchAndStream(
            ChatClient client,
            UserQueryEvent event,
            String sessionId,
            Long userId,
            String searchQuery,
            boolean isNamedEntity,
            UserHealthProfile healthProfile,
            Set<String> excludeIds,
            StringBuilder fullResponse,
            ExecutionTokenUsage stage2Tokens,
            AtomicReference<String> activeModelName
    ) {
        String cleanQuery = (searchQuery != null && !searchQuery.isBlank()) ? searchQuery : event.userQuery();

        UserQueryEvent searchEvent = new UserQueryEvent(
                sessionId, userId, cleanQuery, event.latitude(), event.longitude(),
                event.radiusKm() != null ? event.radiusKm() : DEFAULT_RADIUS_KM, event.ignoreHealthProfile(),
                event.provider()
        );

        log.info("🔍 [V3 Worker] Qdrant Arama Tetikleniyor -> Query: '{}', Lat: {}, Lon: {}, ExcludeCount: {}",
                cleanQuery, event.latitude(), event.longitude(), excludeIds.size());

        SearchResult searchResult = restaurantSearchService.findFreshPlacesV2(
                searchEvent, excludeIds, isNamedEntity, healthProfile
        );

        if (searchResult.documents() == null || searchResult.documents().isEmpty()) {
            String notFoundMsg = "Üzgünüm, belirttiğiniz kriterlere uygun yakınlarda bir mekan bulamadım.";
            fullResponse.append(notFoundMsg);
            notifyUser(userId, StreamEventType.TOKEN, notFoundMsg, false, 0);
            return Collections.emptyList();
        }

        List<RestaurantMetadata> places = searchResult.documents().stream()
                        .map(document -> {
                            RestaurantMetadata meta = objectMapper.convertValue(document.getMetadata(), RestaurantMetadata.class);
                            if (meta.getQdrantId() == null || meta.getQdrantId().isBlank()) {
                                meta.setQdrantId(document.getId());
                            }
                            return meta;
                        }).toList();

        log.info("[V3 Worker] {} mekan bulundu. Canlı Streaming başlatılıyor...", places.size());
        notifyUser(userId, StreamEventType.STATUS, "Mekanlar bulundu, öneriler hazırlanıyor... ✨", false, 0);

        StringBuilder sb = new StringBuilder();

        sb.append("GÜNCEL KULLANICI SORUSU: \"").append(event.userQuery()).append("\"\n\n");
        if (healthProfile != null && healthProfile.isRestricted()) {
            sb.append("KULLANICI SAĞLIK PROFİLİ: ").append(healthProfile).append("\n\n");
        }
        sb.append("BULUNAN MEKANLAR:\n\n");
        for (RestaurantMetadata p : places) {
            sb.append(p.toPresentationMarkdown()).append("\n");
        }

        sb.append("\nTALİMAT: YALNIZCA yukarıdaki mekanları samimi, iştah açıcı ve akıcı bir dille kullanıcıya tanıt.");


        Flux<ChatResponse> responseStream = client.prompt()
                .system(PRESENTATION_SYSTEM_PROMPT)
                .user(sb.toString())
                .stream()
                .chatResponse();

        streamToUser(responseStream, userId, fullResponse, stage2Tokens, activeModelName);

        CostTracker.logSummary("2. Aşama (Streaming)", stage2Tokens.inputTokens.get(), stage2Tokens.outputTokens.get());
        return places;
    }

    private void streamToUser(
            Flux<ChatResponse> stream,
            Long userId,
            StringBuilder fullResponse,
            ExecutionTokenUsage tokenUsage,
            AtomicReference<String> activeModelName
    ) {
        stream.doOnNext(chatResponse -> {
            if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
                String token = chatResponse.getResult().getOutput().getContent();
                if (token != null && !token.isEmpty()) {
                    fullResponse.append(token);
                    notifyUser(userId, StreamEventType.TOKEN, token, false, 0);
                }
            }
            if (chatResponse.getMetadata() != null) {
                if (chatResponse.getMetadata().getModel() != null) {
                    activeModelName.set(chatResponse.getMetadata().getModel());
                }
                if (chatResponse.getMetadata().getUsage() != null) {
                    var usage = chatResponse.getMetadata().getUsage();
                    if (usage.getPromptTokens() != null && usage.getPromptTokens() > 0) {
                        tokenUsage.inputTokens.set(usage.getPromptTokens());
                    }
                    if (usage.getGenerationTokens() != null && usage.getGenerationTokens() > 0) {
                        tokenUsage.outputTokens.set(usage.getGenerationTokens());
                    }
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
            ExecutionTokenUsage stage2Tokens,
            AtomicReference<String> activeModelName
    ) {
        if (activePlaces == null || activePlaces.isEmpty()) {
            String fallback = "Bahsettiğiniz mekanı sohbet geçmişimde bulamadım, tekrar aratmamı ister misiniz?";
            fullResponse.append(fallback);
            notifyUser(userId, StreamEventType.TOKEN, fallback, false, 0);
            return;
        }

        String searchName = (targetPlaceName != null) ? targetPlaceName.toLowerCase(TR) : "";
        RestaurantMetadata targetPlace = activePlaces.stream()
                .filter(p -> p.getName() != null && (p.getName().toLowerCase(TR).contains(searchName)
                        || searchName.contains(p.getName().toLowerCase(TR))))
                .findFirst()
                .orElse(activePlaces.get(0));

        String promptContent = String.format("""
                Kullanıcı Sorusu: "%s"

                [MEKAN BİLGİSİ]:
                %s
                """, userQuery, targetPlace.toPresentationMarkdown());

        Flux<ChatResponse> responseStream = client.prompt()
                .system(CONTEXT_QA_SYSTEM_PROMPT)
                .user(promptContent)
                .stream()
                .chatResponse();

        streamToUser(responseStream, userId, fullResponse, stage2Tokens, activeModelName);

        CostTracker.logSummary("2. Aşama (Context QA Streaming)", stage2Tokens.inputTokens.get(), stage2Tokens.outputTokens.get());
    }

    private void handleDirectReply(Long userId, UserIntentAnalysis intent, StringBuilder fullResponse) {
        String reply = (intent.directReply() != null && !intent.directReply().isBlank())
                ? intent.directReply()
                : "Merhaba! Size en lezzetli mekanları bulmak için buradayım. Ne arzu edersiniz?";
        fullResponse.append(reply);
        notifyUser(userId, StreamEventType.TOKEN, reply, false, 0);
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