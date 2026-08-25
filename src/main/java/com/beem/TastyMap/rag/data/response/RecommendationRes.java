package com.beem.TastyMap.rag.data.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecommendationRes {
    private Long userId;
    private String aiMessage;    // Yapay zekanın ürettiği metin
    private boolean hasMore;      // Frontend buton koysun mu? (true/false)
    private int remainingCount;  // Kalan mekan sayısı
}