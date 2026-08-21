package com.beem.TastyMap.rag.service;

import com.beem.TastyMap.userRelated.health.entitys.UserAllergiesEntity;
import com.beem.TastyMap.userRelated.health.repos.UserAllergiesRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RagPromptBuilder {

    private final UserAllergiesRepo userAllergiesRepo;

    /**
     * Kullanıcının alerji durumuna göre prompt için talimat metni üretir.
     */
    public String buildAllergyInstruction(Long userId, boolean ignoreAllergies) {
        if (ignoreAllergies) {
            return "Kullanıcı alerji filtrelerini kapatmak istedi. Önerilerde kullanıcının alerjilerini DIKKATE ALMA.";
        }

        List<UserAllergiesEntity> userAllergies = userAllergiesRepo.findByUserId(userId);
        if (userAllergies != null && !userAllergies.isEmpty()) {
            String allergiesText = userAllergies.stream()
                    .map(a -> a.getAllergies().getAllergyName())
                    .collect(Collectors.joining(", "));

            return String.format(
                    "KRİTİK GÜVENLİK KURALI: Kullanıcının şu maddelere alerjisi var: [%s]. " +
                            "Aşağıdaki mekanlar arasından bu alerjenleri içeren yemekleri/mekanları KESİNLİKLE önerme ve kullanıcıyı uyar!",
                    allergiesText
            );
        }

        return "Kullanıcının kayıtlı bir alerjisi bulunmamaktadır.";
    }

    /**
     * VectorStore'dan dönen Document listesini tek bir Context metnine dönüştürür.
     */
    public String buildContextText(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "İlgili mekan bilgisi bulunamadı.";
        }
        return documents.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n---\n"));
    }

    /**
     * AI'ya gönderilecek nihai Sistem Prompt'unu hazırlar.
     */
    public String buildSystemPrompt(Long userId, boolean ignoreAllergies, List<Document> documents, String userQuery) {
        String allergyInstruction = buildAllergyInstruction(userId, ignoreAllergies);
        String context = buildContextText(documents);

        return String.format("""
            Sen TastyMap uygulamasının uzman restoran öneri asistanısın.
            
            %s
            
            EK GÜVENLİK VE ISRAR KURALI:
            Eğer kullanıcı alerji uyarısına veya riskine rağmen "yine de öner", "fark etmez ver" gibi bir ifadede bulunuyorsa:
            1. Yanıtına mutlaka kısa ve net bir SAĞLIK VE SORUMLULUK UYARISI ile başla.
            2. Ardından kullanıcının istediği mekanları listele.
            
            Aşağıdaki Mekan Bilgilerini (Context) kullanarak kullanıcıya nazik, iştah açıcı ve öz bir dille yanıt ver:
            ---
            %s
            ---
            
            Kullanıcı Sorusu: %s
            """, allergyInstruction, context, userQuery);
    }
}