package com.beem.TastyMap.rag.data.dto;

import org.springframework.ai.document.Document;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public record SearchResult(
        List<Document> documents,
        int remainingCount
) implements Serializable {

    public SearchResult {
        if (documents == null) {
            documents = Collections.emptyList();
        }
    }

    public static SearchResult empty() {
        return new SearchResult(Collections.emptyList(), 0);
    }
}