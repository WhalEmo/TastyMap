package com.beem.TastyMap.rag.controller;

import com.beem.TastyMap.rag.data.event.UserQueryEvent;
import com.beem.TastyMap.rag.data.request.RecommendationReq;
import com.beem.TastyMap.rag.listener.AiRecommendationV3;
import com.beem.TastyMap.rag.service.RestaurantIndexingService;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rag")
public class RecommendationController {

    private final KafkaTemplate<String, UserQueryEvent> kafkaTemplate;
    private final RestaurantIndexingService service;
    private final AiRecommendationV3 workerV2;

    public RecommendationController(
            KafkaTemplate<String, UserQueryEvent> kafkaTemplate,
            RestaurantIndexingService service,
            AiRecommendationV3 workerV2
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.service = service;
        this.workerV2 = workerV2;
    }

    @PostMapping("/recommend")
    public ResponseEntity<String> askRecommendation(
            @RequestBody RecommendationReq request,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        UserQueryEvent event = new UserQueryEvent(
                request.sessionId(),
                myId,
                request.query(),
                request.latitude(),
                request.longitude(),
                request.radiusKm(),
                request.ignoreHealthProfile(),
                request.provider()
        );

        //kafkaTemplate.send(KafkaConfig.RESTAURANT_AI_REQUESTS_TOPIC, myId.toString(), event);
        workerV2.processAiRequest(event);

        return ResponseEntity.accepted()
                .body("İsteğiniz alındı! Yapay zeka restoranları sizin için inceliyor...");
    }

    @PostMapping("/admin/reindex-all")
    public ResponseEntity<String> reindexAllPlaces() {
        service.indexAllPlacesFromOurDb();
        return ResponseEntity.ok("Veritabanındaki tüm mekanların Qdrant toplu indeksleme (batch) işlemi başlatıldı.");
    }

    @PostMapping("/admin/test-index-sample")
    public ResponseEntity<String> testIndexSamplePlaces(
            @RequestParam(defaultValue = "0") Long lastSeenId,
            @RequestParam(defaultValue = "5") int limit
    ) {
        service.testIndexSamplePlaces(lastSeenId, limit);
        return ResponseEntity.ok(String.format("İlk %d mekan için test indeksleme ve loglama işlemi tamamlandı.", limit));
    }

    @PostMapping("/admin/test-synthetic")
    public ResponseEntity<String> runSyntheticTests() {
        service.runSyntheticExtractionTests();
        return ResponseEntity.ok("Sentetik test senaryoları çalıştırıldı. Konsol loglarını kontrol edebilirsiniz.");
    }

}