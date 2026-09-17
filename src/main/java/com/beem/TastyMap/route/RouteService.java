package com.beem.TastyMap.route;

import com.beem.TastyMap.maps.entity.PlaceEntity;
import com.beem.TastyMap.maps.service.PlacesService;
import com.beem.TastyMap.redis.RedisCacheService;
import com.beem.TastyMap.route.dto.RouteDirectionResponse;
import com.beem.TastyMap.route.dto.osrm.OsrmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);
    private static final String OSRM_BASE_URL = "http://router.project-osrm.org/route/v1/driving";
    private static final long CACHE_TTL_SECONDS = 43200; // 12 Saat

    private final RestClient restClient;
    private final PlacesService placesService;
    private final RedisCacheService redisCacheService;

    public RouteService(
            RestClient.Builder restClientBuilder,
            PlacesService placesService,
            RedisCacheService redisCacheService
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(4));
        factory.setReadTimeout(Duration.ofSeconds(4));

        this.restClient = restClientBuilder.requestFactory(factory).build();
        this.placesService = placesService;
        this.redisCacheService = redisCacheService;
    }

    public RouteDirectionResponse calculateRoute(
            String placeId,
            Double userLat,
            Double userLng,
            boolean forceRefresh
    ) {
        if (userLat == null || userLng == null) {
            return RouteDirectionResponse.empty();
        }

        // 1. Koordinatları ~100m'lik grid hassasiyetine yuvarla
        double roundedLat = Math.round(userLat * 10000.0) / 10000.0;
        double roundedLng = Math.round(userLng * 10000.0) / 10000.0;
        String cacheKey = String.format(Locale.US, "route::place:%s::origin:%.4f_%.4f", placeId, roundedLat, roundedLng);


        if (!forceRefresh) {
            RouteDirectionResponse cached = redisCacheService.get(cacheKey, RouteDirectionResponse.class);
            if (cached != null) {
                log.info("Rota Redis cache üzerinden döndürüldü: {}", cacheKey);
                return cached;
            }
        }

        // 3. Hedef Mekan Koordinatları
        PlaceEntity placeEntity = placesService.findByPlaceId(placeId);
        Double targetLat = placeEntity.getLatitude();
        Double targetLng = placeEntity.getLongitude();

        if (targetLat == null || targetLng == null) {
            return RouteDirectionResponse.empty();
        }

        // 4. OSRM Çağrısı ({startLng},{startLat};{endLng},{endLat})
        String url = String.format(
                Locale.US,
                "%s/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson",
                OSRM_BASE_URL,
                userLng, userLat,
                targetLng, targetLat
        );

        try {
            OsrmResponse osrmResponse = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(OsrmResponse.class);

            if (osrmResponse != null
                    && "Ok".equalsIgnoreCase(osrmResponse.code())
                    && osrmResponse.routes() != null
                    && !osrmResponse.routes().isEmpty()) {

                OsrmResponse.OsrmRoute route = osrmResponse.routes().get(0);
                double distance = route.distance() != null ? route.distance() : 0.0;
                double duration = route.duration() != null ? route.duration() : 0.0;
                List<List<Double>> coordinates = (route.geometry() != null && route.geometry().coordinates() != null)
                        ? route.geometry().coordinates()
                        : List.of();

                RouteDirectionResponse response = new RouteDirectionResponse(
                        distance,
                        duration,
                        formatDistance(distance),
                        formatDuration(duration),
                        coordinates
                );

                // 5. Redis'e Kaydet
                redisCacheService.set(cacheKey, response, CACHE_TTL_SECONDS);

                return response;
            }
        } catch (Exception e) {
            log.error("OSRM rota hesaplama hatası: {}", e.getMessage());
        }

        return RouteDirectionResponse.empty();
    }

    private String formatDistance(double meters) {
        if (meters >= 1000) {
            return String.format(Locale.US, "%.1f km", meters / 1000.0);
        }
        return String.format(Locale.US, "%.0f m", meters);
    }

    private String formatDuration(double seconds) {
        long minutes = Math.round(seconds / 60.0);
        if (minutes >= 60) {
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            return remainingMinutes > 0
                    ? String.format("%d sa %d dk", hours, remainingMinutes)
                    : String.format("%d sa", hours);
        }
        return String.format("%d dk", Math.max(1, minutes));
    }
}