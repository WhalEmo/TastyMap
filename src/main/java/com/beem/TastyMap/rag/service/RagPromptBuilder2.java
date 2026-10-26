package com.beem.TastyMap.rag.service;

import com.beem.TastyMap.rag.data.dto.PresentedPlaceSummary;
import com.beem.TastyMap.user.health.entity.UserAllergiesEntity;
import com.beem.TastyMap.user.health.entity.UserHealthEntity;
import com.beem.TastyMap.visit.dto.VisitResponseDTO;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RagPromptBuilder2 {

    /**
     * 1. SYSTEM PROMPT: Rol, kimlik ve genel diyalog kuralları.
     */
    /*
    public String buildSystemRules(UserDataCollectorService.UserContextData userData, boolean ignoreAllergies) {
        String healthRules = buildHealthAndDietInstruction(userData.healthProfile().toString());
        String allergyRules = buildAllergyInstruction(userData.allergies(), ignoreAllergies);

        return String.format("""
                Sen TastyMap uygulamasının yerel mekan keşif ve gastronomi asistanısın. Doğal, yardımsever ve kurallı bir Türkçe ile konuşursun.

                TEMEL PRENSİPLER:
                1. SADECE sana sağlanan <konusulan_mekanlar> ve <yeni_adaylar> bloklarındaki bilgileri kullan. Asla listede olmayan bir mekan veya özellik uydurma.
                2. NEZAKET VE YENİ İSTEKLER:
                   - Kullanıcı SADECE teşekkür ediyor veya sohbeti kapatıyorsa ("Teşekkürler", "Sağ ol" vb.): Yeni mekan tanıtma, sadece nazikçe rica et.
                   - Kullanıcı HEM teşekkür edip HEM yeni bir mekan/yemek istiyorsa (örn: "Teşekkürler, şimdi de kebapçı önerir misin?"): Nezaketle rica ederek doğrudan aradığı yeni mekanları tanıt.
                3. Kullanıcının aradığı yemek türü (örn: sushi, taco, döner vb.) <yeni_adaylar> listesinde veya yorumlarında YOKSA:
                   -> Kesinlikle listedeki alakasız köfteci, burgerci veya etliekmekçileri önerme!
                   -> Dürüstçe mevcut mekanlar arasında aradığı lezzetin bulunmadığını belirt.
                4. "Size göre çok etkileyici bir istek", "Ben TastyMap asistanıyım" gibi yapay cümleler kurma.
                5. "restaurant, food, point_of_interest, establishment" gibi sistem etiketlerini asla metne yazma.

                %s
                %s

                YENİ MEKAN TANITIM ŞABLONU (Yalnızca yeni mekan önerirken kullan):
                ### [Mekan Adı]
                **Adres:** [Mekanın Adresi]
                [Mekanın ortamını, lezzetlerini ve neden uygun olduğunu anlatan 2-3 cümlelik akıcı açıklama.]
                """, healthRules, allergyRules).replaceAll("\n{3,}", "\n\n").trim();
    }

     */

    /**
     * 2. USER CONTEXT PROMPT: Turn bazlı veriler ve en son talimatlar.
     */
    public String buildUserContextPrompt(
            UserDataCollectorService.UserContextData userData,
            List<PresentedPlaceSummary> sessionKnownPlaces,
            List<Document> freshCandidates,
            String userQuery
    ) {
        String visitsText = buildUserVisitsInstruction(userData.recentVisits());
        String knownPlacesXml = formatKnownPlacesXml(sessionKnownPlaces);
        String freshCandidatesXml = formatFreshCandidatesXml(freshCandidates);

        return String.format("""
                %s
                <konusulan_mekanlar>
                %s
                </konusulan_mekanlar>

                <yeni_adaylar>
                %s
                </yeni_adaylar>

                KULLANICI MESAJI: "%s"

                TALİMATLAR:
                1. KULLANICI TEŞEKKÜR EDİYORSA VEYA VEDALAŞIYORSA:
                   - Yeni bir mekan tanıtma! Nazikçe rica et ve iyi dileklerini ilet.
                2. KULLANICI DAHA ÖNCE BAHSEDİLEN MEKANIN ÖZELLİĞİNİ SORUYORSA (priz, ortam, wifi, saat vb.):
                   - Yalnızca <konusulan_mekanlar> bloğundaki bilgileri kullan.
                   - Bilgilerde priz/wifi kesin geçmiyorsa "priz yoktur" deme; "bilgilerde priz durumu net belirtilmemiş ancak ortam elverişli" şeklinde açıkla. Yeni bir mekan tanıtma!
                3. KULLANICININ İSTEDİĞİ YEMEK <yeni_adaylar> İÇİNDE YOKSA (örn: sushi aranıyor ama listede burger/köfte varsa):
                   - Alakasız mekanları önerme! Aranan lezzetin mevcut mekanlar arasında yer almadığını belirt.
                4. KULLANICI YENİ/ALTERNATİF MEKAN İSTİYORSA:
                   - <yeni_adaylar> içinden kriterlere uyanları seç ve belirlenen şablona sadık kalarak tanıt.
                """, visitsText, knownPlacesXml, freshCandidatesXml, userQuery).trim();
    }

    private String formatKnownPlacesXml(List<PresentedPlaceSummary> places) {
        if (places == null || places.isEmpty()) {
            return "Henüz bir mekan konuşulmadı.";
        }
        StringBuilder sb = new StringBuilder();
        for (PresentedPlaceSummary p : places) {
            sb.append(p.toMarkdown()).append("\n");
        }
        return sb.toString().trim();
    }

    private String formatFreshCandidatesXml(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "Yeni aday mekan bulunamadı.";
        }
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (Document doc : documents) {
            sb.append(String.format("%d. %s\n\n", i++, cleanGoogleTypes(doc.getContent())));
        }
        return sb.toString().trim();
    }

    private String cleanGoogleTypes(String text) {
        if (text == null) return "";
        return text.replaceAll("(?i)\\b(establishment|point_of_interest|food|store|health)\\b,?\\s*", "")
                .replaceAll(",\\s*,", ",")
                .replaceAll(",\\s*$", "")
                .trim();
    }

    private String buildHealthAndDietInstruction(UserHealthEntity health) {
        if (health == null) return "";
        StringBuilder sb = new StringBuilder();
        if (health.getEatType() != null) {
            switch (health.getEatType()) {
                case VEGAN -> sb.append("- KULLANICI VEGAN: Hayvansal ürün odaklı yerleri ele.\n");
                case VEGETARIAN -> sb.append("- KULLANICI VEJETARYEN: Et odaklı yerleri ele.\n");
                default -> {}
            }
        }
        if (health.isHasDiabetes()) {
            sb.append("- DİYABET UYARISI: Aşırı şekerli menüler önerirken uyar.\n");
        }
        return sb.toString().isBlank() ? "" : "SAĞLIK FİLTRELERİ:\n" + sb;
    }

    private String buildAllergyInstruction(List<UserAllergiesEntity> userAllergies, boolean ignoreAllergies) {
        if (ignoreAllergies || userAllergies == null || userAllergies.isEmpty()) return "";
        String allergiesText = userAllergies.stream()
                .map(a -> a.getAllergies().getAllergyName())
                .collect(Collectors.joining(", "));
        return String.format("ALERJİ UYARISI: Kullanıcının [%s] alerjisi bulunmaktadır!\n", allergiesText);
    }

    private String buildUserVisitsInstruction(List<VisitResponseDTO> recentVisits) {
        if (recentVisits == null || recentVisits.isEmpty()) return "";
        String visitsText = recentVisits.stream()
                .map(v -> String.format("- %s (%s)", v.getPlaceName(), cleanGoogleTypes(v.getCategories())))
                .collect(Collectors.joining("\n"));
        return "DAMAK TADI GEÇMİŞİ:\n" + visitsText + "\n";
    }
}