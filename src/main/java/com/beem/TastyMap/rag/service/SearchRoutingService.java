package com.beem.TastyMap.rag.service;

import com.beem.TastyMap.rag.data.dto.ChatHistoryMessage;
import com.beem.TastyMap.rag.data.enums.MessageRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class SearchRoutingService {

    private static final Logger log = LoggerFactory.getLogger(SearchRoutingService.class);
    private final ChatClient chatClient;

    public SearchRoutingService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public boolean requiresNewSearch(String userQuery, List<ChatHistoryMessage> chatHistory) {
        if (chatHistory == null || chatHistory.isEmpty() || !hasAnyAssistantRecommendation(chatHistory)) {
            log.info("Router: Konuşma geçmişi boş veya öneri yok, zorunlu YENİ ARAMA (true).");
            return true;
        }

        // Son asistan cevabında geçen mekan adını çek
        String lastAssistantText = findLastAssistantMessage(chatHistory);
        String lastPlace = extractPlaceName(lastAssistantText);

        String prompt = """
                Sen bir RAG arama yönlendiricisisin.
                
                KONUŞULAN SON MEKAN: "%s"
                KULLANICININ YENİ MESAJI: "%s"

                SORU: Kullanıcının bu mesajını yanıtlamak için veritabanında YENİ bir mekan araması yapılmalı mı?
                - Eğer kullanıcı yukarıdaki mekanın (oranın/buranın) prizini, internetini, fiyatını, menüsünü, ortamını soruyorsa: YENİ ARAMA GEREKMEZ.
                - Eğer kullanıcı başka bir yer, yeni bir kafe/restoran veya alternatif istiyorsa: YENİ ARAMA GEREKİR.

                CEVAP FORMATI:
                Sadece tek bir kelime yaz: 'ARAMA_YAP' veya 'DETAY_SORUSU'.
                """.formatted(lastPlace, userQuery);

        try {
            String rawDecision = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // İŞTE İSTEDİĞİN HAM MODEL CEVABI LOGU:
            log.info("ROUTER MODELİNDEN GELEN HAM CEVAP: [{}] (Query: '{}')", rawDecision, userQuery);

            if (rawDecision == null || rawDecision.isBlank()) {
                return true;
            }

            String normalized = rawDecision.trim().toUpperCase(Locale.ROOT);

            // Eğer model DETAY, HAYIR veya ARAMA_YAPMA dediyse arama yapma!
            if (normalized.contains("DETAY") || normalized.contains("HAYIR") || normalized.contains("GEREKMEZ")) {
                log.info("Router Kararı: DETAY_SORUSU algılandı -> Qdrant pas geçilecek.");
                return false;
            }

            // Eğer açıkça ARAMA veya EVET dediyse
            if (normalized.contains("ARAMA_YAP") || normalized.contains("EVET") || normalized.contains("YENİ")) {
                log.info("Router Kararı: YENİ ARAMA algılandı -> Qdrant çalıştırılacak.");
                return true;
            }

            // Belirsiz bir cümle kurduysa logla ve varsayılan davran
            log.warn("Router belirsiz cevap verdi: [{}], fallback olarak arama yapılacak.", rawDecision);
            return true;

        } catch (Exception e) {
            log.warn("Router çağrısında hata: {}, fallback olarak arama yapılacak.", e.getMessage());
            return true;
        }
    }

    private boolean hasAnyAssistantRecommendation(List<ChatHistoryMessage> history) {
        return history.stream().anyMatch(m -> m.role() == MessageRole.ASSISTANT && m.content().contains("###"));
    }

    private String findLastAssistantMessage(List<ChatHistoryMessage> history) {
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).role() == MessageRole.ASSISTANT) {
                return history.get(i).content();
            }
        }
        return "";
    }

    private String extractPlaceName(String text) {
        if (text == null || text.isBlank()) return "Bilinmeyen Mekan";
        for (String line : text.lines().toList()) {
            String trimmed = line.trim();
            if (trimmed.startsWith("###")) {
                return trimmed.replaceAll("###", "").replaceAll("📍", "").trim();
            }
        }
        return "Bilinmeyen Mekan";
    }
}