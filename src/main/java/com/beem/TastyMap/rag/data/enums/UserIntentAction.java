package com.beem.TastyMap.rag.data.enums;

public enum UserIntentAction {

    /**
     * Kullanıcı sohbette zaten önerilmiş bir mekanın detayını soruyor.
     * Örn: "Müto cafede priz var mı?", "Quba kaça kadar açık?", "Oraya nasıl giderim?"
     * Davranış: Qdrant aranmaz! Hafızadaki mekan verisiyle doğrudan cevap üretilir.
     */
    CONTEXT_QA,

    /**
     * Kullanıcı spesifik bir dükkan/mekan adını arıyor.
     * Örn: "Müto Cafe nerede?", "Karahan Döner açık mı?", "Nane'i 7 Cafe'ye bakalım"
     * Davranış: İsim bazlı Qdrant araması (Mesafe filtresi gevşek, exclude listesi YOK).
     */
    NAMED_ENTITY_SEARCH,

    /**
     * Genel lezzet, mutfak veya kategori keşif araması.
     * Örn: "Güzel bir kebapçı öner", "Hafif tatlı ve filtre kahve", "Akşam ne yesek"
     * Davranış: Temiz Qdrant vektör araması (exclude listesi YOK, en iyi 3 mekan).
     */
    NEW_SEARCH,

    /**
     * Önerilen mekanları beğenmeyip alternatif veya devamını isteme.
     * Örn: "Başka ne var?", "Farklı bir yer göster", "Daha ucuz bir alternatif"
     * Davranış: Sayfalama (Sadece burada daha önce gösterilen mekanlar dışlanır).
     */
    FETCH_MORE,

    /**
     * Sohbet, selamlama veya sistem soruları.
     * Örn: "Merhaba", "Sen kimsin?", "Teşekkürler", "Görüşürüz"
     * Davranış: Qdrant aranmaz, model doğrudan sıcak cevap döner.
     */
    CHITCHAT
}