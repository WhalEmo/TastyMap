package com.beem.TastyMap.rag.controller;

import com.beem.TastyMap.rag.config.KafkaConfig;
import com.beem.TastyMap.rag.data.event.UserQueryEvent;
import com.beem.TastyMap.rag.data.request.RecommendationReq;
import com.beem.TastyMap.rag.service.BulkIndexingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
public class RecommendationController {

    private final KafkaTemplate<String, UserQueryEvent> kafkaTemplate;
    private final BulkIndexingService bulkIndexingService;

    @PostMapping("/recommend")
    public ResponseEntity<String> askRecommendation(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody RecommendationReq request) {

        UserQueryEvent event = new UserQueryEvent(
                userId,
                request.getQuery(),
                request.isIgnoreAllergies(),
                request.getLatitude(),
                request.getLongitude(),
                request.getRadiusKm()
        );

        kafkaTemplate.send(KafkaConfig.RESTAURANT_AI_REQUESTS_TOPIC, userId.toString(), event);

        return ResponseEntity.accepted()
                .body("İsteğiniz alındı! Yapay zeka restoranları sizin için inceliyor...");
    }

    /**
     * Veritabanındaki tüm mekanları Qdrant'a topluca yüklemek için tetikleyici endpoint
     */
    @PostMapping("/admin/reindex-all")
    public ResponseEntity<String> reindexAllPlaces() {
        bulkIndexingService.indexAllPlacesFromOurDb();
        return ResponseEntity.ok("Veritabanındaki tüm mekanların Qdrant toplu indeksleme (batch) işlemi başlatıldı.");
    }
}