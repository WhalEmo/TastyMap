package com.beem.TastyMap.rag.data.dto;

public record RewrittenSearchQuery(
        String searchQuery,        // Qdrant'a atılacak temiz arama ifadesi
        SearchIntent intent,       // SEARCH_NEW veya ASK_DETAIL
        String focusedPlaceName    // Detay soruluyorsa mekan adı, yoksa null
) {
    public enum SearchIntent {
        SEARCH_NEW, // Yeni mekan arayışı veya alternatif isteme
        ASK_DETAIL  // Daha önce önerilen mekan hakkında detay sorma (priz, wifi, fiyat vb.)
    }
}