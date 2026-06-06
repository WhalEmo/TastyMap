package com.beem.TastyMap.maps.data;

import com.beem.TastyMap.maps.data.geojson.Feature;
import com.beem.TastyMap.maps.data.geojson.FeatureCollection;
import com.beem.TastyMap.maps.data.geojson.Geometry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PlaceMapper {

    public FeatureCollection convertToGeoJson(List<PlaceResult> results) {
        FeatureCollection collection = new FeatureCollection();

        for (PlaceResult place : results) {
            Feature feature = new Feature();

            feature.setGeometry(new Geometry(
                    place.getGeometry().getLocation().getLng(),
                    place.getGeometry().getLocation().getLat()
            ));

            feature.addProperty("id", place.getPlace_id());
            feature.addProperty("name", place.getName());
            feature.addProperty("rating", place.getRating());
            feature.addProperty("status", place.getBusiness_status());

            if (place.getTypes() != null && !place.getTypes().isEmpty()) {
                feature.addProperty("category", place.getTypes().get(0));
            }
            feature.addProperty("min_zoom", calculateMinZoom(place));
            feature.addProperty("priority", (place.getRating() != null ? place.getRating() : 0));

            collection.addFeature(feature);
        }
        return collection;
    }

    private int calculateMinZoom(PlaceResult place) {
        double rating = (place.getRating() != null) ? place.getRating() : 0.0;
        int reviewCount = (place.getUser_ratings_total() != null) ? place.getUser_ratings_total() : 0;

        if (rating >= 4.5 || reviewCount > 500) return 10;
        if (rating >= 4.0) return 12;
        if (rating >= 3.0) return 14;
        return 15;
    }

}
