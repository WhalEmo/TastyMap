package com.beem.TastyMap.rag.data;

import java.util.List;

public record SearchSession(
        String query,
        List<String> placeIds,
        int currentIndex
) {
    public SearchSession withCurrentIndex(int newIndex) {
        return new SearchSession(this.query, this.placeIds, newIndex);
    }
}