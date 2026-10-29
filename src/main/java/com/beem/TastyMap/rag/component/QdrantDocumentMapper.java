package com.beem.TastyMap.rag.component;

import com.beem.TastyMap.rag.data.document.RestaurantDocument;
import com.beem.TastyMap.rag.data.document.RestaurantMetadata;
import com.beem.TastyMap.rag.qdrant.QdrantPayloadConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qdrant.client.grpc.Points;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class QdrantDocumentMapper {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final ObjectMapper objectMapper;
    private final QdrantPayloadConverter payloadConverter;

    public QdrantDocumentMapper(
            ObjectMapper objectMapper,
            QdrantPayloadConverter payloadConverter
    ) {
        this.objectMapper = objectMapper;
        this.payloadConverter = payloadConverter;
    }

    public RestaurantDocument toRestaurantDocument(Points.ScoredPoint point) {

        Map<String, Object> rawMap = payloadConverter.toPlainMap(point.getPayloadMap());

        RestaurantMetadata meta = objectMapper.convertValue(rawMap, RestaurantMetadata.class);

        meta.setVectorScore((double) point.getScore());

        String rawContent = Optional.ofNullable(rawMap.get("content"))
                .or(() -> Optional.ofNullable(rawMap.get("doc_content")))
                .map(String::valueOf)
                .orElse("");

        String id = point.getId().hasUuid() ? point.getId().getUuid() : String.valueOf(point.getId().getNum());
        meta.setQdrantId(id);

        return new RestaurantDocument(id, rawContent, meta);
    }

    public RestaurantDocument toRestaurantDocument(Points.ScoredPoint point, Double userLat, Double userLon) {
        RestaurantDocument doc = toRestaurantDocument(point);
        RestaurantMetadata meta = doc.getMetadata();
        if (userLat != null && userLon != null && meta.getLatitude() != null && meta.getLongitude() != null) {
            double distanceKm = calculateHaversineKm(userLat, userLon, meta.getLatitude(), meta.getLongitude());
            meta.setDistanceKm(distanceKm);
        }

        return doc;
    }

    public double calculateHaversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public RestaurantMetadata fromMetadataMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return new RestaurantMetadata();
        }

        return objectMapper.convertValue(map, RestaurantMetadata.class);
    }
}