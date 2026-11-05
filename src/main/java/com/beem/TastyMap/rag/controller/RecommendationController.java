package com.beem.TastyMap.rag.controller;

import com.beem.TastyMap.rag.data.event.UserQueryEvent;
import com.beem.TastyMap.rag.data.request.RecommendationReq;
import com.beem.TastyMap.rag.listener.AiRecommendationV3;
import com.beem.TastyMap.rag.service.RestaurantIndexingService;
import com.beem.TastyMap.sse.SseEmitterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/ai/chat")
public class RecommendationController {

    private static final Logger log = LoggerFactory.getLogger(RecommendationController.class);

    private final KafkaTemplate<String, UserQueryEvent> kafkaTemplate;
    private final RestaurantIndexingService service;
    private final AiRecommendationV3 workerV2;
    private final AsyncTaskExecutor taskExecutor;
    private final SseEmitterManager emitterManager;

    public RecommendationController(
            KafkaTemplate<String, UserQueryEvent> kafkaTemplate,
            RestaurantIndexingService service,
            AiRecommendationV3 workerV2,
            AsyncTaskExecutor taskExecutor,
            SseEmitterManager emitterManager
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.service = service;
        this.workerV2 = workerV2;
        this.taskExecutor = taskExecutor;
        this.emitterManager = emitterManager;
    }

    @PostMapping(value = "/recommend", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> askRecommendation(
            @RequestBody RecommendationReq request,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();

        SseEmitter emitter = emitterManager.createEmitter(myId);

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

        taskExecutor.execute(()-> {
            try {
                workerV2.processAiRequest(event);
            } catch (Exception e) {
                log.error("❌ [RAG Error] User: {}", myId, e);
            }
        });

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform")
                .header(HttpHeaders.CONNECTION, "keep-alive")
                .header("X-Accel-Buffering", "no")
                .body(emitter);
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