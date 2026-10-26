package com.beem.TastyMap.rag.data.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public record PresentedPlaceSummary(
        String qdrantId,             // Qdrant UUID
        String placeId,              // Google Place ID (ChIJ...)
        Long dbId,                   // TastyMap MySQL ID
        String name,                 // Mekan Adı
        String address,              // Açık Adres
        Double latitude,             // Harita pini için lat
        Double longitude,            // Harita pini için lon
        Double distanceKm,           // Kullanıcıya hesaplanan kuş uçuşu mesafe
        Double googleRating,         // Google Puanı (Örn: 4.4)
        Double tastymapRating,       // TastyMap Puanı
        Boolean isOpenNow,           // Açık/Kapalı durumu
        List<String> categories,     // ["restaurant", "cafe"]
        List<String> standoutDishes, // ["hamburger", "taco", "churros"]
        String ambiance,             // "nezih, sessiz, ders çalışmaya uygun"
        String warnings,             // "Dikkat: sıra bekleniyor, otopark sorunu var"
        PlaceScoresMeta scores,      // Detay puanları (Hizmet, Lezzet vb.)
        String highlightReviews      // Müşteri yorum notları
) implements Serializable {

    /**
     * LLM'e beslenecek yüksek sinyal/gürültü oranına sahip hiyerarşik format.
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();

        // 1. Başlık ve Temel Rozetler
        sb.append("### **").append(name != null ? name : "Mekan").append("**");

        List<String> badges = new ArrayList<>();
        if (distanceKm != null) {
            badges.add(String.format("📍 %.1f km uzaklıkta", distanceKm));
        }
        if (googleRating != null && googleRating > 0) {
            badges.add(String.format("%.1f ⭐", googleRating));
        }
        if (isOpenNow != null) {
            badges.add(isOpenNow ? "Açık" : "Kapalı");
        }

        if (!badges.isEmpty()) {
            sb.append(" (").append(String.join(" | ", badges)).append(")");
        }
        sb.append("\n");

        // 2. Öne Çıkan Lezzetler (Modelin doğrudan hamburgeri/yemeği yakalamasını sağlar)
        if (standoutDishes != null && !standoutDishes.isEmpty()) {
            sb.append(String.format("  - **Öne Çıkan Lezzetler:** %s\n", String.join(", ", standoutDishes)));
        }

        // 3. Ortam & Ambiyans
        if (ambiance != null && !ambiance.isBlank()) {
            sb.append(String.format("  - **Ortam:** %s\n", ambiance));
        }

        // 4. Uyarılar (Varsa model kullanıcıyı samimi şekilde uyarır)
        if (warnings != null && !warnings.isBlank()) {
            sb.append(String.format("  - **Dikkat Edilmesi Gereken:** %s\n", warnings));
        }

        // 5. Adres & Kategori
        if (address != null && !address.isBlank()) {
            sb.append(String.format("  - **Adres:** %s\n", address));
        }

        // 6. Yorum Özetleri
        if (highlightReviews != null && !highlightReviews.isBlank()) {
            sb.append(String.format("  - **Yorum Notları:** %s\n", highlightReviews.trim()));
        }

        return sb.toString();
    }
}