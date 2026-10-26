package com.beem.TastyMap.rag.service;

import com.beem.TastyMap.user.health.entity.UserAllergiesEntity;
import com.beem.TastyMap.user.health.entity.UserHealthEntity;
import com.beem.TastyMap.user.health.repo.UserAllergiesRepo;
import com.beem.TastyMap.user.health.repo.UserHealthRepo;
import com.beem.TastyMap.visit.dto.VisitResponseDTO;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RagPromptBuilder {

    private final UserAllergiesRepo userAllergiesRepo;
    private final UserHealthRepo userHealthRepo;

    public RagPromptBuilder(UserAllergiesRepo userAllergiesRepo, UserHealthRepo userHealthRepo) {
        this.userAllergiesRepo = userAllergiesRepo;
        this.userHealthRepo = userHealthRepo;
    }


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
    /*
    public String buildSystemPrompt(UserDataCollectorService.UserContextData userData,
                                    boolean ignoreAllergies,
                                    List<Document> documents,
                                    String userQuery,
                                    boolean isExhausted,
                                    int remainingCount) {

        String allergyInstruction = buildAllergyInstruction(userData.allergies(), ignoreAllergies);
        String healthAndDietInstruction = buildHealthAndDietInstruction(userData.healthInfo().orElse(null));
        String visitsInstruction = buildUserVisitsInstruction(userData.recentVisits());
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

     */

    /**
     * AI'ya gönderilecek nihai Sistem Prompt'unu hazırlar.
     */
    /*
    public String buildSystemPromptV2(UserDataCollectorService.UserContextData userData,
                                    boolean ignoreAllergies,
                                    List<Document> documents,
                                    String userQuery,
                                    boolean isExhausted,
                                    int remainingCount) {

        String allergyInstruction = buildAllergyInstruction(userData.allergies(), ignoreAllergies);
        String healthAndDietInstruction = buildHealthAndDietInstruction(userData.healthInfo().orElse(null));
        String visitsInstruction = buildUserVisitsInstruction(userData.recentVisits());
        String context = buildContextText(documents);

        String paginationInstruction = isExhausted
                ? "DURUM BİLGİSİ: Bu aramada konuma uygun gösterilebilecek SON mekanları sunuyorsun. Mekanları tanıttıktan sonra kriterlerdeki mekanların bittiğini, isterse farklı bir arama yapabileceğini nazikçe belirt."
                : String.format("DURUM BİLGİSİ: Önerilerini sunduktan sonra, kullanıcının istemesi halinde bu konumda %d tane daha alternatif mekan gösterebileceğini kısaca hatırlat.", remainingCount);

        return String.format("""
        Sen TastyMap uygulamasının uzman gastronomi, restoran ve mekan keşif asistanısın.

        GÖREV TANIMI VE KAPSAM:
        - Görevin; restoranlar, kafeler, kahveciler, pastaneler, ders çalışmaya/vakit geçirmeye uygun mekanlar, menüler ve mutfak kültürü hakkında öneri vermektir.
        - Kullanıcının sakinlik, ders çalışma, priz, Wi-Fi, kahve veya yemek gibi mekan ortamına dair istekleri doğrudan senin uzmanlık alanındadır.
        - SADECE yemek, kafe ve mekan sektörüyle tamamen alakasız konularda (kodlama, tıp, siyaset, spor vb.) nazikçe şu yanıtı ver: "Ben TastyMap'in lezzet ve mekan asistanıyım! Yalnızca restoran, kafe ve mekan önerileri konusunda yardımcı olabilirim. Bugün nasıl bir mekan arıyorsunuz?"

        ÖNERİ VE SUNUM KURALLARI:
        1. SADECE aşağıda verilen Context içerisindeki gerçek mekanları kullan. Asla hayali mekan veya özellik uydurma.
        2. Context içinde sana sağlanan mekanları dikkatlice incele. Kullanıcının arayışına (örn: kahve, sakin ortam, yemek türü vb.) hitap eden mekanları tek tek tanıt.
        3. Mekanları tanıtırken Context'te yer alan 'Kullanıcı Puan Ortalamaları' (Atmosfer, Lezzet, Servis vb.) ve müşteri yorumlarından faydalanarak neden uygun olduklarını gerekçelendir.
        4. Her mekan için net bir başlık aç ve düzenli madde işaretleriyle sun.

        SAĞLIK VE BESLENME TERCİHLERİ:
        %s

        ALERJİ TALİMATI:
        %s

        ZİYARET GEÇMİŞİ TALİMATI:
        %s

        SAYFALAMA VE KALAN MEKAN DURUMU:
        %s

        AŞAĞIDA KULLANICININ ARAMASINA UYGUN MEKANLAR (CONTEXT) BULUNMAKTADIR:
        ---
        %s
        ---

        Kullanıcı Sorusu: %s
        """, healthAndDietInstruction, allergyInstruction, visitsInstruction, paginationInstruction, context, userQuery);
    }

    */

    public String buildHealthAndDietInstruction2(UserHealthEntity health) {
        if (health == null) {
            return ""; // Boşsa prompt'u kirletme
        }

        StringBuilder sb = new StringBuilder();

        if (health.getEatType() != null) {
            switch (health.getEatType()) {
                case VEGAN -> sb.append("- KESİN FİLTRE: Kullanıcı VEGAN beslenmektedir. Et, tavuk, balık, süt ürünleri, yumurta odaklı mekanları (ciğerci, köfteci, kebapçı vb.) KESİNLİKLE ELE!\n");
                case VEGETARIAN -> sb.append("- KESİN FİLTRE: Kullanıcı VEJETARYEN beslenmektedir. Et, tavuk, balık odaklı mekanları KESİNLİKLE ELE!\n");
                default -> {} // Normal ise yazmaya gerek yok
            }
        }

        if (health.isHasDiabetes()) {
            sb.append("- SAĞLIK UYARISI: Kullanıcının DİYABETİ vardır. Tatlı/şeker odaklı yerler önerirken mutlaka dikkatli olması gerektiğini belirt.\n");
        }

        return sb.toString().isBlank() ? "" : "SAĞLIK VE BESLENME KURALLARI:\n" + sb;
    }
}