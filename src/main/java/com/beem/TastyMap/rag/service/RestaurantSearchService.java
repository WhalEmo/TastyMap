package com.beem.TastyMap.rag.service;

import com.beem.TastyMap.rag.component.HealthSafetyGuard;
import com.beem.TastyMap.rag.component.QdrantDocumentMapper;
import com.beem.TastyMap.rag.component.RestaurantReRanker;
import com.beem.TastyMap.rag.data.document.RestaurantDocument;
import com.beem.TastyMap.rag.data.document.RestaurantMetadata;
import com.beem.TastyMap.rag.data.document.VenueCategory;
import com.beem.TastyMap.rag.data.dto.SearchResult;
import com.beem.TastyMap.rag.data.dto.UserHealthProfile;
import com.beem.TastyMap.rag.data.event.UserQueryEvent;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RestaurantSearchService {

    private static final Logger log = LoggerFactory.getLogger(RestaurantSearchService.class);
    private static final int CANDIDATE_LIMIT = 20;
    private static final int PRESENT_LIMIT = 3;
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double CITY_WIDE_RADIUS_KM = 15.0;

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final QdrantClient qdrantClient;
    private final QdrantDocumentMapper documentMapper;
    private final HealthSafetyGuard healthSafetyGuard;
    private final RestaurantReRanker reRanker;

    @Value("${spring.ai.vectorstore.qdrant.collection-name:restaurants}")
    private String collectionName;

    public RestaurantSearchService(
            VectorStore vectorStore,
            EmbeddingModel embeddingModel,
            QdrantClient qdrantClient,
            QdrantDocumentMapper documentMapper,
            HealthSafetyGuard healthSafetyGuard,
            RestaurantReRanker reRanker
    ) {
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
        this.qdrantClient = qdrantClient;
        this.documentMapper = documentMapper;
        this.healthSafetyGuard = healthSafetyGuard;
        this.reRanker = reRanker;
    }

    /**
     * Vektör tabanından adayları çeker, mesafeye göre süzer ve
     * bu sohbette daha önce önerilmiş mekanların tamamını tavizsiz eler.
     */
    public SearchResult findFreshPlaces(UserQueryEvent event, Set<String> alreadyPresentedPlaceIds) {
        // 1. Vektör tabanından geniş havuz çek
        SearchRequest searchRequest = SearchRequest.query(event.userQuery()).withTopK(CANDIDATE_LIMIT);
        List<Document> rawResults = vectorStore.similaritySearch(searchRequest);

        if (rawResults == null || rawResults.isEmpty()) {
            return SearchResult.empty();
        }

        // 2. Coğrafi Mesafe Kontrolü
        double radius = (event.radiusKm() != null && event.radiusKm() > 0) ? event.radiusKm() : 10.0;
        List<Document> distanceFiltered = rawResults;

        if (event.latitude() != null && event.longitude() != null) {
            distanceFiltered = rawResults.stream()
                    .filter(doc -> isWithinDistance(doc, event.latitude(), event.longitude(), radius))
                    .collect(Collectors.toList());

            if (distanceFiltered.isEmpty()) {
                log.warn("{} km içinde mekan bulunamadı, genel sonuçlar kullanılıyor.", radius);
                distanceFiltered = rawResults;
            }
        }

        // 3. KESİN ELEME: Bu sohbette daha önce adı geçen mekanların Qdrant UUID'lerini sil
        Set<String> excludedIds = (alreadyPresentedPlaceIds != null) ? alreadyPresentedPlaceIds : Collections.emptySet();
        List<Document> freshCandidates = distanceFiltered.stream()
                .filter(doc -> !excludedIds.contains(doc.getId()))
                .collect(Collectors.toList());

        if (freshCandidates.isEmpty()) {
            return SearchResult.empty();
        }

        // 4. Modele sunulacak ilk sayfayı (3 mekan) ayır
        int takeCount = Math.min(PRESENT_LIMIT, freshCandidates.size());
        List<Document> page = freshCandidates.subList(0, takeCount);
        int remaining = freshCandidates.size() - takeCount;

        return new SearchResult(page, remaining);
    }

    public SearchResult findFreshPlacesV2(
            UserQueryEvent event,
            Set<String> alreadyPresentedPlaceIds,
            boolean isNamedEntity,
            UserHealthProfile healthProfile
    ) {
        if (event == null || event.userQuery() == null || event.userQuery().isBlank()) {
            return SearchResult.empty();
        }
        log.info("🔎 [SearchService] Gelen Sorgu (Embedding Metni): '{}'", event.userQuery());
        log.info("📍 [SearchService] Konum Filtresi -> Lat: {}, Lon: {}, Yarıçap: {} km, İsim Araması Mı: {}",
                event.latitude(), event.longitude(), event.radiusKm(), isNamedEntity);
        try {
            // 1. Kullanıcı sorgusunu BGE-M3 ile 1024 boyutlu float vektöre dönüştür
            float[] queryVectorFloats = embeddingModel.embed(event.userQuery());
            List<Float> vectorList = new ArrayList<>(queryVectorFloats.length);
            for (float f : queryVectorFloats) {
                vectorList.add(f);
            }

            // 2. Qdrant SearchBuilder Hazırla
            Points.SearchPoints.Builder searchBuilder = Points.SearchPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .addAllVector(vectorList)
                    .setLimit(isNamedEntity ? 3 : CANDIDATE_LIMIT)
                    .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build());

            // 3. Filtreleri İnşa Et
            Points.Filter filter = buildQdrantFilter(event, alreadyPresentedPlaceIds, isNamedEntity, healthProfile);
            searchBuilder.setFilter(filter);

            // 4. Qdrant'a gRPC üzerinden sorguyu at
            List<Points.ScoredPoint> scoredPoints = qdrantClient.searchAsync(searchBuilder.build()).get();

            if (scoredPoints == null || scoredPoints.isEmpty()) {
                log.info("Qdrant V2 aramasında kriterlere uygun mekan bulunamadı. (Sorgu: {})", event.userQuery());
                return SearchResult.empty();
            }

            // 5. Qdrant ScoredPoint nesnelerini Spring AI Document formatına dönüştür
            List<RestaurantDocument> freshCandidates = scoredPoints.stream()
                    .map(p -> documentMapper.toRestaurantDocument(p, event.latitude(), event.longitude()))
                    .toList();

            List<RestaurantDocument> safeCandidates = healthSafetyGuard.filterSafePlaces(
                    freshCandidates,
                    healthProfile,
                    event.userQuery()
            );

            List<Document> finalDocumentList;
            if (isNamedEntity) {
                int takeCount = Math.min(PRESENT_LIMIT, safeCandidates.size());
                finalDocumentList = safeCandidates.stream()
                        .limit(takeCount)
                        .map(RestaurantDocument::toSpringDocument)
                        .toList();
            } else {
                finalDocumentList = safeCandidates.stream()
                        .peek(doc -> reRanker.rank(doc, event.userQuery(), healthProfile))
                        .sorted(Comparator.comparing(RestaurantDocument::getCompositeScore).reversed())
                        .limit(PRESENT_LIMIT)
                        .map(RestaurantDocument::toSpringDocument)
                        .toList();
            }

            int remaining = safeCandidates.size() - finalDocumentList.size();

            log.info("Qdrant V2 arama başarılı. Toplam aday: {}, Sunulan: {}, Kalan: {}",
                    freshCandidates.size(), finalDocumentList.size(), remaining);

            return new SearchResult(finalDocumentList, remaining);

        } catch (Exception e) {
            log.error("findFreshPlacesV2 yürütülürken hata oluştu: {}", e.getMessage(), e);
            // Hata durumunda güvenli fallback olarak V1'e yönlendirilebilir
            return findFreshPlaces(event, alreadyPresentedPlaceIds);
        }
    }

    private Points.Filter buildQdrantFilter(
            UserQueryEvent event,
            Set<String> alreadyPresentedPlaceIds,
            boolean isNamedEntity,
            UserHealthProfile healthProfile
    ) {
        Points.Filter.Builder filterBuilder = Points.Filter.newBuilder();


        filterBuilder.addMust(Points.Condition.newBuilder()
                .setField(Points.FieldCondition.newBuilder()
                        .setKey("is_closed")
                        .setMatch(Points.Match.newBuilder().setBoolean(false).build())
                        .build())
                .build());



        // 2. İsim araması değilse, sadece yeme-içme mekanlarını getir
        if (!isNamedEntity) {
            filterBuilder.addMust(Points.Condition.newBuilder()
                    .setField(Points.FieldCondition.newBuilder()
                            .setKey("is_dining")
                            .setMatch(Points.Match.newBuilder().setBoolean(true).build())
                            .build())
                    .build());
        }

        // 3. Coğrafi Mesafe (Native GeoRadius)
        if (!isNamedEntity && event.latitude() != null && event.longitude() != null) {
            double radiusKm = (event.radiusKm() != null && event.radiusKm() > 0) ? event.radiusKm() : CITY_WIDE_RADIUS_KM;
            float radiusMeters = (float) (radiusKm * 1000.0);

            filterBuilder.addMust(Points.Condition.newBuilder()
                    .setField(Points.FieldCondition.newBuilder()
                            .setKey("location")
                            .setGeoRadius(Points.GeoRadius.newBuilder()
                                    .setCenter(Points.GeoPoint.newBuilder()
                                            .setLat(event.latitude())
                                            .setLon(event.longitude())
                                            .build())
                                    .setRadius(radiusMeters)
                                    .build())
                            .build())
                    .build());
        }

        // 4. Daha önce önerilenleri doğrudan dışla (must_not)
        if (alreadyPresentedPlaceIds != null && !alreadyPresentedPlaceIds.isEmpty()) {
            List<Points.PointId> excludePointIds = alreadyPresentedPlaceIds.stream()
                    .map(id -> Points.PointId.newBuilder().setUuid(id).build())
                    .toList();

            filterBuilder.addMustNot(Points.Condition.newBuilder()
                    .setHasId(Points.HasIdCondition.newBuilder()
                            .addAllHasId(excludePointIds)
                            .build())
                    .build());
        }

        applyDietaryConstraints(filterBuilder, event.userQuery().toLowerCase(Locale.ROOT), healthProfile);

        return filterBuilder.build();
    }

    private void applyDietaryConstraints(
            Points.Filter.Builder filterBuilder,
            String queryLower,
            UserHealthProfile healthProfile
    ) {
        boolean isVeganSearch = queryLower.contains("vegan") ||
                (healthProfile != null && Boolean.TRUE.equals(healthProfile.isVegan()));

        boolean isVegetarianSearch = !isVeganSearch && (queryLower.contains("vejetaryen") || queryLower.contains("vejeteryan") ||
                (healthProfile != null && Boolean.TRUE.equals(healthProfile.isVegetarian())));

        boolean isGlutenFreeSearch = queryLower.contains("gluten") || queryLower.contains("çölyak") || queryLower.contains("colyak") ||
                (healthProfile != null && Boolean.TRUE.equals(healthProfile.isCeliac()));

        if (isVeganSearch) {
            log.info("🥗 [SearchFilter] Vegan kısıtı devrede. Et ve balık mekanları eleniyor.");
            filterBuilder.addMust(Points.Condition.newBuilder()
                    .setField(Points.FieldCondition.newBuilder()
                            .setKey("is_vegan_friendly")
                            .setMatch(Points.Match.newBuilder().setBoolean(true).build())
                            .build())
                    .build());

            // Ocakbaşı ve kebapçıları kesin olarak engelle
            filterBuilder.addMustNot(Points.Condition.newBuilder()
                    .setField(Points.FieldCondition.newBuilder()
                            .setKey("venue_category")
                            .setMatch(Points.Match.newBuilder().setKeyword(VenueCategory.MEAT_AND_GRILL.getKey()).build())
                            .build())
                    .build());
        }
        // --- VEJETARYEN FİLTRESİ ---
        else if (isVegetarianSearch) {
            log.info("🥦 [SearchFilter] Vejetaryen kısıtı devrede.");
            filterBuilder.addMust(Points.Condition.newBuilder()
                    .setField(Points.FieldCondition.newBuilder()
                            .setKey("is_vegetarian_friendly")
                            .setMatch(Points.Match.newBuilder().setBoolean(true).build())
                            .build())
                    .build());

            filterBuilder.addMustNot(Points.Condition.newBuilder()
                    .setField(Points.FieldCondition.newBuilder()
                            .setKey("venue_category")
                            .setMatch(Points.Match.newBuilder().setKeyword(VenueCategory.MEAT_AND_GRILL.getKey()).build())
                            .build())
                    .build());
        }

        // --- GLUTENSİZ / ÇÖLYAK FİLTRESİ ---
        if (isGlutenFreeSearch) {
            log.info("🌾 [SearchFilter] Glutensiz kısıtı devrede.");
            filterBuilder.addMust(Points.Condition.newBuilder()
                    .setField(Points.FieldCondition.newBuilder()
                            .setKey("is_gluten_free_friendly")
                            .setMatch(Points.Match.newBuilder().setBoolean(true).build())
                            .build())
                    .build());
        }

        if (healthProfile != null && healthProfile.hasDiabetes() && !healthSafetyGuard.isExplicitSweetSearch(queryLower)) {
            log.info("🩺 [SearchFilter] Diyabet kısıtı devrede. Tatlı/Pastane odaklı mekanlar eleniyor.");
            filterBuilder.addMustNot(Points.Condition.newBuilder()
                    .setField(Points.FieldCondition.newBuilder()
                            .setKey("venue_category")
                            .setMatch(Points.Match.newBuilder().setKeyword(VenueCategory.BAKERY_AND_DESSERT.getKey()).build())
                            .build())
                    .build());
        }
    }

    private boolean isWithinDistance(Document doc, double userLat, double userLon, double maxKm) {
        Object latObj = doc.getMetadata().get("latitude");
        Object lonObj = doc.getMetadata().get("longitude");

        if (latObj == null || lonObj == null) {
            return true;
        }

        try {
            double docLat = Double.parseDouble(latObj.toString());
            double docLon = Double.parseDouble(lonObj.toString());

            double dLat = Math.toRadians(docLat - userLat);
            double dLon = Math.toRadians(docLon - userLon);

            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                    + Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(docLat))
                    * Math.sin(dLon / 2) * Math.sin(dLon / 2);

            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return (EARTH_RADIUS_KM * c) <= maxKm;
        } catch (Exception e) {
            return true;
        }
    }
}