package com.beem.TastyMap.rag.service;

import com.beem.TastyMap.rag.data.dto.ChatHistoryMessage;
import com.beem.TastyMap.rag.data.dto.RewrittenSearchQuery;
import com.beem.TastyMap.rag.data.enums.MessageRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class QueryRewriterService {

    private static final Logger log = LoggerFactory.getLogger(QueryRewriterService.class);
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QueryRewriterService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }


    public RewrittenSearchQuery analyzeAndRewrite(String currentQuery, List<ChatHistoryMessage> chatHistory) {
        if (chatHistory == null || chatHistory.isEmpty()) {
            return new RewrittenSearchQuery(currentQuery, RewrittenSearchQuery.SearchIntent.SEARCH_NEW, null);
        }

        String lastAssistantResponse = findLastAssistantMessage(chatHistory);
        String lastPlaceName = extractPlaceNameFromText(lastAssistantResponse);

        String systemPrompt = """
                Sen bir arama niyeti analiz asistanısın. Kullanıcının mesajını ve son konuşulan mekanı analiz edip SADECE geçerli bir JSON üretirsin.

                KURALLAR:
                1. Eğer kullanıcı "başka", "farklı", "yeni", "alternatif", "değiştir" diyorsa veya yeni bir mekan arıyorsa:
                   - intent = "SEARCH_NEW"
                   - focusedPlaceName = null
                   - searchQuery = Aranan mekan türü ve şehir/bölge (Asla eski mekan adını içermez!)

                2. Eğer kullanıcı mevcut mekanın özelliğini soruyorsa (priz var mı, fiyatı nasıl, menüsü ne, kaça kadar açık, wifi var mı):
                   - intent = "ASK_DETAIL"
                   - focusedPlaceName = Son konuşulan mekanın adı
                   - searchQuery = Mekan adı + sorulan özellik

                ÖRNEKLER:
                Son Mekan: "Coffeemania Next Konya"
                Kullanıcı: "Peki oranın priz durumu nasıl?"
                JSON: {"intent":"ASK_DETAIL","focusedPlaceName":"Coffeemania Next Konya","searchQuery":"Coffeemania Next Konya priz durumu"}

                Son Mekan: "Coffeemania Next Konya"
                Kullanıcı: "Başka mekan önerebilir misin bu civarlarda"
                JSON: {"intent":"SEARCH_NEW","focusedPlaceName":null,"searchQuery":"Konya civarında sakin ders çalışma kafeleri"}

                Son Mekan: "Coffeemania Next Konya"
                Kullanıcı: "Başka kahve mekanı önerebilir misin"
                JSON: {"intent":"SEARCH_NEW","focusedPlaceName":null,"searchQuery":"Konya alternatif kahve mekanları kafeler"}
                """;

        String userPrompt = String.format("""
                Son Konuşulan Mekan: "%s"
                Kullanıcının Yeni Mesajı: "%s"
                JSON:""", lastPlaceName, currentQuery);

        try {
            String rawJson = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            if (rawJson != null && !rawJson.isBlank()) {
                String cleanJson = rawJson.replace("```json", "").replace("```", "").trim();
                RewrittenSearchQuery result = objectMapper.readValue(cleanJson, RewrittenSearchQuery.class);

                // GÜVENLİK FİLTRESİ (Guardrail):
                // Eğer kullanıcı açıkça "başka", "farklı", "alternatif" yazdıysa, model yanlışlıkla ASK_DETAIL dese bile SEARCH_NEW'e zorla!
                String queryLower = currentQuery.toLowerCase();
                if (queryLower.contains("başka") || queryLower.contains("farklı") || queryLower.contains("alternatif")) {
                    result = new RewrittenSearchQuery(result.searchQuery(), RewrittenSearchQuery.SearchIntent.SEARCH_NEW, null);
                }

                log.info("Sorgu Analiz Edildi -> Intent: {}, Focused: {}, Query: '{}'",
                        result.intent(), result.focusedPlaceName(), result.searchQuery());
                return result;
            }
        } catch (Exception e) {
            log.warn("Sorgu analizi başarısız oldu, varsayılan akışa dönülüyor: {}", e.getMessage());
        }

        return new RewrittenSearchQuery(currentQuery, RewrittenSearchQuery.SearchIntent.SEARCH_NEW, null);
    }

    private String findLastAssistantMessage(List<ChatHistoryMessage> history) {
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).role() == MessageRole.ASSISTANT) {
                return history.get(i).content();
            }
        }
        return "";
    }

    private String extractPlaceNameFromText(String text) {
        if (text == null || text.isBlank()) return "";
        for (String line : text.lines().toList()) {
            String trimmed = line.trim();
            if (trimmed.startsWith("###")) {
                return trimmed.replaceAll("###", "").replaceAll("📍", "").trim();
            }
        }
        return "";
    }

    /**
     * Kullanıcının sorusunu konuşma geçmişini baz alarak
     * vektör tabanının anlayacağı bağımsız bir arama sorgusuna dönüştürür.
     */
    public String rewriteQuery(String currentQuery, List<ChatHistoryMessage> chatHistory) {
        // Eğer geçmiş yoksa ilk sorudur, yeniden yazmaya gerek yok
        if (chatHistory == null || chatHistory.isEmpty()) {
            return currentQuery;
        }

        // Son 2-3 mesajı al (Bağlam için yeterlidir)
        List<Message> messages = new ArrayList<>();
        int startIndex = Math.max(0, chatHistory.size() - 4);

        for (int i = startIndex; i < chatHistory.size(); i++) {
            ChatHistoryMessage msg = chatHistory.get(i);
            if (msg.role() == MessageRole.USER) {
                messages.add(new UserMessage(msg.content()));
            } else if (msg.role() == MessageRole.ASSISTANT) {
                // Sadece mekan adlarının geçtiği ilk satırları almak yeterli
                String snippet = msg.content().lines().limit(3).reduce("", (a, b) -> a + " " + b);
                messages.add(new AssistantMessage(snippet));
            }
        }

        String instruction = String.format("""
                GÖREV: Kullanıcının son mesajını, konuşma geçmişindeki bağlamı (özellikle bahsedilen mekan adı, şehir ve konuyu) kullanarak TEK BAŞINA ANLAŞILIR bir arama sorgusuna dönüştür.
                
                KURALLAR:
                1. Asla cevap verme, selamlama yapma veya açıklama ekleme.
                2. Zamirleri (o, orası, oranın, burası vb.) konuşmada geçen gerçek mekan adıyla değiştir.
                3. SADECE yeniden yazılmış 1 cümlelik arama ifadesini yaz.
                
                Kullanıcının Son Mesajı: "%s"
                Yeniden Yazılmış Arama Sorgusu:""", currentQuery);

        try {
            String rewritten = chatClient.prompt()
                    .messages(messages)
                    .user(instruction)
                    .call()
                    .content();

            if (rewritten != null && !rewritten.isBlank()) {
                String cleanQuery = rewritten.replaceAll("\"", "").trim();
                log.info("Sorgu Yeniden Yazıldı: '{}' -> '{}'", currentQuery, cleanQuery);
                return cleanQuery;
            }
        } catch (Exception e) {
            log.warn("Sorgu yeniden yazılırken hata oluştu, orijinal sorgu kullanılıyor: {}", e.getMessage());
        }

        return currentQuery;
    }
}