package com.beem.TastyMap.rag.component;

import com.beem.TastyMap.rag.data.document.RestaurantDocument;
import com.beem.TastyMap.rag.data.document.RestaurantMetadata;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

@Component
public class QdrantDocumentMapper {

    private static final double EARTH_RADIUS_KM = 6371.0;

    public RestaurantDocument toRestaurantDocument(Points.ScoredPoint point) {
        RestaurantMetadata meta = new RestaurantMetadata();
        String rawContent = "";

        for (Map.Entry<String, JsonWithInt.Value> entry : point.getPayloadMap().entrySet()) {
            String key = entry.getKey();
            JsonWithInt.Value val = entry.getValue();

            switch (key) {
                case "content", "doc_content" -> rawContent = val.getStringValue();
                case "name" -> meta.setName(val.getStringValue());
                case "place_id" -> meta.setPlaceId(val.getStringValue());
                case "db_id" -> meta.setDbId(val.getIntegerValue());
                case "google_rating" -> meta.setGoogleRating(val.getDoubleValue());
                case "tastymap_rating" -> meta.setTastymapRating(val.getDoubleValue());
                case "types" -> {
                    if (val.hasStringValue() && !val.getStringValue().isBlank()) {
                        meta.setTypes(Arrays.stream(val.getStringValue().split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .toList());
                    }
                }
                case "location" -> {
                    if (val.hasStructValue()) {
                        Map<String, JsonWithInt.Value> fields = val.getStructValue().getFieldsMap();
                        if (fields.containsKey("lat")) meta.setLatitude(fields.get("lat").getDoubleValue());
                        if (fields.containsKey("lon")) meta.setLongitude(fields.get("lon").getDoubleValue());
                    }
                }
            }
        }
        meta.setVectorScore((double) point.getScore());


        String id = point.getId().hasUuid() ? point.getId().getUuid() : String.valueOf(point.getId().getNum());
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
}