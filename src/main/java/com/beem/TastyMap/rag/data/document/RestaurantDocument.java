package com.beem.TastyMap.rag.data.document;

import org.springframework.ai.document.Document;

public class RestaurantDocument {

    private final String id;
    private final String rawContent;
    private final RestaurantMetadata metadata;

    public RestaurantDocument(String id, String rawContent, RestaurantMetadata metadata) {
        this.id = id;
        this.rawContent = rawContent != null ? rawContent : "";
        this.metadata = metadata != null ? metadata : new RestaurantMetadata();
    }

    public String getId() { return id; }
    public String getRawContent() { return rawContent; }
    public RestaurantMetadata getMetadata() { return metadata; }

    public double getCompositeScore() {
        return metadata.getCompositeScore() != null ? metadata.getCompositeScore() : 0.0;
    }

    public void setCompositeScore(double score) {
        this.metadata.setCompositeScore(score);
    }

    public Document toSpringDocument() {
        String enrichedContent = rawContent;
        if (metadata.getDistanceKm() != null) {
            enrichedContent += String.format("\nMesafe: Kullanıcıya %.1f km uzaklıkta.\n", metadata.getDistanceKm());
        }
        return new Document(id, enrichedContent, metadata.toMap());
    }
}