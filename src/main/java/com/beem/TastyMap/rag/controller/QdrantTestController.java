package com.beem.TastyMap.rag.controller;

import com.beem.TastyMap.rag.data.dto.SearchResult;
import com.beem.TastyMap.rag.data.dto.UserHealthProfile;
import com.beem.TastyMap.rag.data.event.UserQueryEvent;
import com.beem.TastyMap.rag.service.RestaurantSearchService;
import com.beem.TastyMap.rag.service.UserDataCollectorService;
import com.beem.TastyMap.user.health.model.HealthEnum;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/test/qdrant")
public class QdrantTestController {

    private final QdrantClient qdrantClient;
    private final EmbeddingModel embeddingModel;
    private final RestaurantSearchService restaurantSearchService;
    private final UserDataCollectorService userDataCollectorService;

    @Value("${spring.ai.vectorstore.qdrant.collection-name:restaurants}")
    private String collectionName;

    public QdrantTestController(
            QdrantClient qdrantClient,
            EmbeddingModel embeddingModel,
            RestaurantSearchService restaurantSearchService,
            UserDataCollectorService userDataCollectorService
    ) {
        this.qdrantClient = qdrantClient;
        this.embeddingModel = embeddingModel;
        this.restaurantSearchService = restaurantSearchService;
        this.userDataCollectorService = userDataCollectorService;
    }

    /**
     * GET Araması (Hem Düz Semantik Hem Geo-Radius Destekler)
     *
     * Örnek 1 (Sadece Semantik):
     * GET /api/test/qdrant/search?q=sessiz kafe&topK=3
     *
     * Örnek 2 (2 km Yarıçaplı Konum Filtreli):
     * GET /api/test/qdrant/search?q=sessiz kafe&lat=37.867&lon=32.481&radius=2000&topK=3
     */
    @GetMapping("/search")
    public ResponseEntity<List<DocumentTestResponse>> searchGet(
            @RequestParam("q") String query,
            @RequestParam(value = "lat", required = false) Double lat,
            @RequestParam(value = "lon", required = false) Double lon,
            @RequestParam(value = "radius", defaultValue = "3000") double radiusMeters,
            @RequestParam(value = "topK", defaultValue = "5") int topK) {

        return ResponseEntity.ok(executeHybridSearch(query, lat, lon, radiusMeters, topK));
    }

    /**
     * POST Araması (JSON Body)
     */
    @PostMapping("/search")
    public ResponseEntity<List<DocumentTestResponse>> searchPost(@RequestBody TestSearchRequest request) {
        int topK = request.topK() != null && request.topK() > 0 ? request.topK() : 5;
        double radius = request.radiusMeters() != null && request.radiusMeters() > 0 ? request.radiusMeters() : 3000.0;

        return ResponseEntity.ok(executeHybridSearch(
                request.query(),
                request.latitude(),
                request.longitude(),
                radius,
                topK
        ));
    }

    private List<DocumentTestResponse> executeHybridSearch(
            String query,
            Double lat,
            Double lon,
            double radiusMeters,
            int topK) {

        try {
            // 1. Sorgu metnini BGE-M3 ile 1024 boyutlu vektöre çevir
            float[] queryVectorFloats = embeddingModel.embed(query);
            List<Float> vectorList = new ArrayList<>(queryVectorFloats.length);
            for (float f : queryVectorFloats) {
                vectorList.add(f);
            }

            // 2. Qdrant Search Builder'ı kur
            Points.SearchPoints.Builder searchBuilder = Points.SearchPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .addAllVector(vectorList)
                    .setLimit(topK)
                    .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build());

            // 3. Koordinat geldiyse Native Geo-Radius Filtresi ekle
            if (lat != null && lon != null) {
                Points.Filter geoFilter = Points.Filter.newBuilder()
                        .addMust(Points.Condition.newBuilder()
                                .setField(Points.FieldCondition.newBuilder()
                                        .setKey("location")
                                        .setGeoRadius(Points.GeoRadius.newBuilder()
                                                .setCenter(Points.GeoPoint.newBuilder()
                                                        .setLat(lat)
                                                        .setLon(lon)
                                                        .build())
                                                .setRadius((float) radiusMeters)
                                                .build())
                                        .build())
                                .build())
                        .build();

                searchBuilder.setFilter(geoFilter);
            }

            // 4. Qdrant'a gRPC üzerinden sorguyu at
            List<Points.ScoredPoint> scoredPoints = qdrantClient.searchAsync(searchBuilder.build()).get();

            // 5. Sonuçları DTO listesine dönüştür
            List<DocumentTestResponse> results = new ArrayList<>();
            for (Points.ScoredPoint point : scoredPoints) {
                Map<String, Object> metadata = new HashMap<>();
                String content = "";

                for (Map.Entry<String, io.qdrant.client.grpc.JsonWithInt.Value> entry : point.getPayloadMap().entrySet()) {
                    if ("content".equals(entry.getKey()) || "doc_content".equals(entry.getKey())) {
                        content = entry.getValue().getStringValue();
                    } else {
                        metadata.put(entry.getKey(), extractValue(entry.getValue()));
                    }
                }

                metadata.put("score", point.getScore());
                String id = point.getId().hasUuid() ? point.getId().getUuid() : String.valueOf(point.getId().getNum());

                results.add(new DocumentTestResponse(id, content, metadata));
            }

            return results;

        } catch (Exception e) {
            throw new RuntimeException("Qdrant hibrit arama başarısız oldu: " + e.getMessage(), e);
        }
    }

    private Object extractValue(io.qdrant.client.grpc.JsonWithInt.Value val) {
        if (val.hasStringValue()) return val.getStringValue();
        if (val.hasIntegerValue()) return val.getIntegerValue();
        if (val.hasDoubleValue()) return val.getDoubleValue();
        if (val.hasBoolValue()) return val.getBoolValue();
        if (val.hasStructValue()) {
            Map<String, Object> map = new HashMap<>();
            val.getStructValue().getFieldsMap().forEach((k, v) -> map.put(k, extractValue(v)));
            return map;
        }
        return val.toString();
    }

    // DTO Tanımları
    public record TestSearchRequest(
            String query,
            Double latitude,
            Double longitude,
            Double radiusMeters,
            Integer topK
    ) {}

    public record DocumentTestResponse(
            String id,
            String content,
            Map<String, Object> metadata
    ) {}

    @GetMapping("/ai/health")
    public void healthAi(
            Authentication authentication
    ) {
        userDataCollectorService.collectUserData(
                (Long) authentication.getPrincipal()
        );
    }


    @GetMapping("/v2")
    public ResponseEntity<Map<String, Object>> testSearchV2(
            @RequestParam("query") String query,
            @RequestParam(value = "lat", defaultValue = "37.945083") double lat,
            @RequestParam(value = "lon", defaultValue = "32.517995") double lon,
            @RequestParam(value = "radiusKm", required = false) Double radiusKm,
            @RequestParam(value = "isNamedEntity", defaultValue = "false") boolean isNamedEntity
    ) {
        UserQueryEvent event = new UserQueryEvent(
                UUID.randomUUID().toString(),
                1L,
                query,
                false,
                false,
                lat,
                lon,
                radiusKm
        );

        UserHealthProfile profile = new UserHealthProfile(
                HealthEnum.NORMAL,
                false,
                Set.of()
        );

        SearchResult result = restaurantSearchService.findFreshPlacesV2(
                event,
                Collections.emptySet(),
                isNamedEntity,
                profile
        );

        // 1. Tüm dökümanları topla
        List<Map<String, Object>> detailedCandidates = new ArrayList<>();

        // 2. Metadata içindeki tüm alanları, tiplerini ve örnek değerlerini analiz et
        Map<String, Map<String, Object>> detectedSchema = new LinkedHashMap<>();

        for (Document doc : result.documents()) {
            Map<String, Object> meta = doc.getMetadata();

            // Her bir metadata anahtarını incele
            for (Map.Entry<String, Object> entry : meta.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                detectedSchema.computeIfAbsent(key, k -> {
                    Map<String, Object> fieldInfo = new LinkedHashMap<>();
                    fieldInfo.put("type", value != null ? value.getClass().getSimpleName() : "null");
                    fieldInfo.put("sample_value", value);
                    return fieldInfo;
                });
            }

            Map<String, Object> item = new HashMap<>();
            item.put("id", doc.getId());
            item.put("content", doc.getContent());
            item.put("metadata", meta);
            detailedCandidates.add(item);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tested_query", query);
        response.put("coordinates", Map.of("lat", lat, "lon", lon));
        response.put("radius_km", radiusKm);
        response.put("is_named_entity", isNamedEntity);
        response.put("total_found", detailedCandidates.size());
        response.put("remaining_count", result.remainingCount());

        // EN ÜSTTE ŞEMA VE TİPLERİ GÖSTEREN ANALİZ BLOĞU
        response.put("detected_metadata_schema", detectedSchema);
        response.put("candidates", detailedCandidates);

        return ResponseEntity.ok(response);
    }
}