package com.beem.TastyMap.rag.service;
import com.beem.TastyMap.maps.data.PlaceDetailsResult;
import com.beem.TastyMap.maps.entity.PlaceEntity;
import com.beem.TastyMap.maps.repository.PlaceRepo;
import com.beem.TastyMap.mapsReview.ReviewRepo;
import com.beem.TastyMap.mapsReview.data.ReviewResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkIndexingService {

    private final RestaurantIndexingService restaurantIndexingService;
    private final PlaceRepo placeRepository;
    private final ReviewRepo reviewRepository;

    private static final int BATCH_SIZE = 100; // Her pakette Qdrant'a 100 mekan gönderilecek


    //BURASI KENDI MEKANLARIMIIZ VE ONLAIRN YORUMLARNNI VEKTOR DBYE ALDIIGMIZ METOD
    @Async
    public void indexAllPlacesFromOurDb() {
        log.info("Veritabanındaki tüm mekanlar Qdrant için BATCH (paketler) halinde indekslenmeye başlandı...");

        List<PlaceEntity> dbPlaces = placeRepository.findAll();
        List<Document> batchList = new ArrayList<>();

        int count = 0;
        for (PlaceEntity place : dbPlaces) {
            try {
                List<ReviewResult> reviews = reviewRepository.findReviewResultsByPlaceId(place.getPlaceId());
                PlaceDetailsResult placeDetails = PlaceDetailsResult.fromEntity(place);

                Document doc = restaurantIndexingService.createDocument(placeDetails, reviews);
                batchList.add(doc);
                count++;

                // Paket boyutu 100'e ulaştığında Qdrant'a topluca gönder
                if (batchList.size() >= BATCH_SIZE) {
                    restaurantIndexingService.indexPlacesBatch(batchList);
                    batchList.clear(); // Paketi sıfırla
                }

            } catch (Exception e) {
                log.error("Mekan dönüştürülürken/indekslenirken hata (Place ID: {}): {}", place.getPlaceId(), e.getMessage());
            }
        }

        // Döngü bittiğinde elde kalan son mekanları da Qdrant'a gönder
        if (!batchList.isEmpty()) {
            restaurantIndexingService.indexPlacesBatch(batchList);
        }

        log.info("Toplu (Batch) indeksleme tamamlandı. Toplam indekslenen mekan sayısı: {}", count);
    }
}