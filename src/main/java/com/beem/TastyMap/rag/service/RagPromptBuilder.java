package com.beem.TastyMap.rag.service;

import com.beem.TastyMap.userRelated.health.entitys.UserAllergiesEntity;
import com.beem.TastyMap.userRelated.health.entitys.UserHealthEntity;
import com.beem.TastyMap.userRelated.health.repos.UserAllergiesRepo;
import com.beem.TastyMap.userRelated.health.repos.UserHealthRepo;
import com.beem.TastyMap.userRelated.visit.VisitResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RagPromptBuilder {

    private final UserAllergiesRepo userAllergiesRepo;
    private final UserHealthRepo userHealthRepo;


    public String buildHealthAndDietInstruction(UserHealthEntity health) {
        if (health == null) {
            return "Kullanıcının özel bir beslenme tercihi veya diyabet durumu bulunmamaktadır.";
        }

        StringBuilder sb = new StringBuilder("BESLENME VE SAĞLIK FİLTRELERİ:\n");

        if (health.getEatType() != null) {
            switch (health.getEatType()) {
                case VEGAN -> sb.append("- KESİN KURAL: Kullanıcı VEGAN beslenmektedir. Et, tavuk, balık, süt ürünleri, yumurta vb. içeren mekanları/menüleri KESİNLİKLE önerme!\n");
                case VEGETARIAN -> sb.append("- KESİN KURAL: Kullanıcı VEJETARYEN beslenmektedir. Et, tavuk, balık içeren yemekleri önerme!\n");
                default -> sb.append("- Beslenme Tipi: Normal.\n");
            }
        }

        if (health.isHasDiabetes()) {
            sb.append("- ÖNEMLİ SAĞLIK UYARISI: Kullanıcının DİYABETİ bulunmaktadır. Yüksek şekerli/karbonhidratlı menüleri önerme veya uyar.\n");
        }

        return sb.toString();
    }

    public String buildUserVisitsInstruction(List<VisitResponseDTO> recentVisits) {
        if (recentVisits == null || recentVisits.isEmpty()) {
            return "Kullanıcının henüz geçmiş ziyaret kaydı bulunmamaktadır.";
        }

        String visitsText = recentVisits.stream()
                .map(v -> String.format("- %s (Mutfak: %s)", v.getPlaceName(), v.getCategories()))
                .collect(Collectors.joining("\n"));

        return String.format("""
                KULLANICININ GEÇMİŞTE GİTTİĞİ MEKANLAR VE SEVDİĞİ MUTFAKLAR:
                %s
                
                DAMAK TADI VE MUTFAK ANALİZİ TALİMATI:
                - Yukarıdaki geçmiş bilgileri SADECE kullanıcının ne tür yemekler ve mutfaklar (örn: İtalyan, Ev Yemekleri, Burger vb.) sevdiğini anlamak için kullan.
                - Geçmiş mekanların konumunu veya şehirlerini KESİNLİKLE dikkate alma! Şehir önerilerini yalnızca verilen Context ve Kullanıcı Konumu'na göre yap.
                """, visitsText);
    }

    public String buildAllergyInstruction(List<UserAllergiesEntity> userAllergies, boolean ignoreAllergies) {
        if (ignoreAllergies) {
            return "Kullanıcı alerji filtrelerini kapatmak istedi. Önerilerde alerjileri DİKKATE ALMA.";
        }

        if (userAllergies != null && !userAllergies.isEmpty()) {
            String allergiesText = userAllergies.stream()
                    .map(a -> a.getAllergies().getAllergyName())
                    .collect(Collectors.joining(", "));

            return String.format("KRİTİK GÜVENLİK KURALI: Kullanıcının şu maddelere alerjisi var: [%s]. Bunları içeren mekanları KESİNLİKLE önerme ve kullanıcıyı uyar!", allergiesText);
        }

        return "Kullanıcının kayıtlı bir alerjisi bulunmamaktadır.";
    }


    public String buildContextText(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "İlgili mekan bilgisi bulunamadı.";
        }
        return documents.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n---\n"));
    }

    /**
     * AI'ya gönderilecek nihai Sistem Prompt'unu hazırlar (Pagination desteği eklendi).
     */
    public String buildSystemPrompt(UserDataCollectorService.UserContextData userData,
                                    boolean ignoreAllergies,
                                    List<Document> documents,
                                    String userQuery,
                                    boolean isExhausted,
                                    int remainingCount) {

        String allergyInstruction = buildAllergyInstruction(userData.getAllergies(), ignoreAllergies);
        String healthAndDietInstruction = buildHealthAndDietInstruction(userData.getHealthInfo().orElse(null));
        String visitsInstruction = buildUserVisitsInstruction(userData.getRecentVisits());
        String context = buildContextText(documents);

        String paginationInstruction = isExhausted
                ? "ÖNEMLİ DURUM BILGİSİ: Bu aramada konuma yakın gösterilebilecek SON MEKANLARI sunuyorsun. Mekanları tanıttıktan sonra kullanıcıya dürüstçe bu konumdaki ve kriterlerdeki tüm mekanların bittiğini, isterse farklı bir arama veya yemek türü söyleyebileceğini nazikçe belirt."
                : String.format("ÖNEMLİ DURUM BİLGİSİ: Kullanıcıya mekanları sunduktan sonra, isterse bu konumda %d tane daha alternatif mekan gösterebileceğini hatırlat.", remainingCount);

        return String.format("""
        Sen SADECE ve SADECE TastyMap uygulamasının uzman restoran ve yemek öneri asistanısın.
        
        KESİN KURALLAR:
        1. Senin tek görevin yemek, restoran, mekan, kafe, mutfak kültürü ve menüler hakkında bilgi vermektir.
        2. Yemek veya restoranlar dışındaki HER HANGİ BİR KONUDA (Oyun, kodlama, siyaset, genel sohbet, hava durumu vb.) gelen soruları KESİNLİKLE YANITLAMA.
        3. Konu dışı bir soru geldiğinde nazikçe şu cevabı ver: "Ben TastyMap'in lezzet asistanıyım! Yalnızca restoran, yemek ve mekan önerileri konusunda size yardımcı olabilirim. Bugün ne yemek istersiniz?"
        4. SADECE verilen Context içerisindeki gerçek mekanları kullan, asla haritadan veya hafızandan olmayan restoran uydurma.

        SAĞLIK VE BESLENME TERCİHLERİ:
        %s
        
        ALERJİ TALİMATI:
        %s
        
        ZİYARET GEÇMİŞİ TALİMATI:
        %s

        MEKAN YÖNETİMİ VE SAYFALAMA DURUMU:
        %s
        
        Aşağıda kullanıcının konumuna yakın mekanlar (Context) listelenmiştir. Bu bilgileri kullanarak nazik, iştah açıcı ve öz bir dille yanıt ver:
        ---
        %s
        ---
        
        Kullanıcı Sorusu: %s
        """,healthAndDietInstruction, allergyInstruction, visitsInstruction, paginationInstruction, context, userQuery);
    }
}