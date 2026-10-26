package com.beem.TastyMap.rag.data.response;

public record AiStreamPayload(
        Long userId,
        StreamEventType type, // STATUS, TOKEN, COMPLETED, ERROR
        String content,       // O anki kelime veya durum mesajı
        boolean hasMore,      // Daha fazla mekan var mı?
        int remainingCount    // Kalan mekan sayısı
) {
    public enum StreamEventType {
        STATUS,     // "Yakınınızdaki mekanlar taranıyor...", "Öneriler hazırlanıyor..."
        TOKEN,      // Canlı akan her bir kelime/hece
        COMPLETED,  // Akış bitti (Footer mesajı ve buton bilgisi taşır)
        ERROR       // Hata durumu
    }
}