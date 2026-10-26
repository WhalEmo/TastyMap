package com.beem.TastyMap.rag.service;

import com.beem.TastyMap.maps.data.PlaceDetailsResult;
import com.beem.TastyMap.mapsReview.data.ReviewResult;
import com.beem.TastyMap.mapsReview.enums.ScoreType;
import com.beem.TastyMap.rag.data.document.RestaurantDocument;
import com.beem.TastyMap.rag.data.document.RestaurantMetadata;
import com.beem.TastyMap.rag.data.dto.IndexedPlaceDTO;
import com.beem.TastyMap.rag.data.dto.IndexedReviewDTO;
import com.beem.TastyMap.rag.data.dto.IndexedScoreDTO;
import com.beem.TastyMap.rag.data.dto.PlaceSummaryExtraction;
import com.beem.TastyMap.rag.factory.AiProviderFactory;
import com.beem.TastyMap.rag.repository.PlaceIndexingQueryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RestaurantIndexingService {

    private static final Logger log = LoggerFactory.getLogger(RestaurantIndexingService.class);

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final PlaceIndexingQueryRepository placeIndexingQueryRepository;
    private final AiProviderFactory aiProviderFactory;

    private static final int CHUNK_SIZE = 100;
    private static final int MAX_REVIEWS_FOR_EMBEDDING = 10;
    private static final int MAX_REVIEW_CHAR_LENGTH = 300;

    public RestaurantIndexingService(
            VectorStore vectorStore,
            PlaceIndexingQueryRepository placeIndexingQueryRepository,
            ChatClient.Builder chatClientBuilder, AiProviderFactory aiProviderFactory
    ) {
        this.vectorStore = vectorStore;
        this.placeIndexingQueryRepository = placeIndexingQueryRepository;
        chatClient = chatClientBuilder
                .defaultOptions(
                        OllamaOptions.create()
                                .withTemperature(0.0f)
                                .withTopP(0.1f)
                                .withNumPredict(350)
                )
                .build();
        this.aiProviderFactory = aiProviderFactory;
    }



    @Async
    public void indexAllPlacesFromOurDb() {
        log.info("Keyset Cursor destekli toplu indeksleme (Batch Chunking) başlatıldı...");

        Long lastSeenId = 0L;
        int totalProcessed = 0;
        int chunkNumber = 1;

        while (true) {
            List<IndexedPlaceDTO> chunk = placeIndexingQueryRepository.fetchNextChunk(lastSeenId, CHUNK_SIZE);
            if (chunk == null || chunk.isEmpty()) {
                break;
            }

            List<Long> chunkIds = chunk.stream().map(IndexedPlaceDTO::id).toList();

            Map<Long, Set<String>> typesMap = placeIndexingQueryRepository.fetchTypesForPlaceIds(chunkIds);

            List<Document> documents = new ArrayList<>(chunk.size());
            for (IndexedPlaceDTO dto : chunk) {
                try {
                    Set<String> types = typesMap.getOrDefault(dto.id(), Collections.emptySet());
                    Document doc = buildDocumentFromDtoV2(dto, types);
                    logGeneratedContent(doc);
                    documents.add(doc);
                } catch (Exception e) {
                    log.error("Mekan dönüştürülürken hata (PlaceId: {}): {}", dto.placeId(), e.getMessage());
                }
            }
            // 4. Qdrant'a toplu bas
            if (!documents.isEmpty()) {
                indexPlacesBatch(documents);
                totalProcessed += documents.size();
                log.info("Paket #{} tamamlandı. (İşlenen: {}, Son DB ID: {})",
                        chunkNumber, totalProcessed, chunk.get(chunk.size() - 1).id());
            }

            // 5. Cursor güncelle
            lastSeenId = chunk.get(chunk.size() - 1).id();
            chunkNumber++;
        }
        log.info("Toplu indeksleme başarıyla bitti! Qdrant'a aktarılan toplam mekan sayısı: {}", totalProcessed);
    }


    private Document buildDocumentFromDto(IndexedPlaceDTO place, Set<String> types) {
        String qdrantDocId = UUID.nameUUIDFromBytes(place.placeId().getBytes(StandardCharsets.UTF_8)).toString();

        StringBuilder textBuilder = new StringBuilder();
        textBuilder.append("Restoran: ").append(place.name()).append("\n");

        String typesJoined = (types != null && !types.isEmpty()) ? String.join(", ", types) : "";
        if (!typesJoined.isBlank()) {
            textBuilder.append("Kategoriler: ").append(typesJoined).append("\n");
        }
        if (place.formattedAddress() != null) {
            textBuilder.append("Adres: ").append(place.formattedAddress()).append("\n");
        }

        String scoreSummary = calculateAverageScores(place.reviews());
        if (!scoreSummary.isBlank()) {
            textBuilder.append("Kullanıcı Puan Ortalamaları: ").append(scoreSummary).append("\n");
        }

        String cleanedReviews = extractCleanReviewsFromDto(place.reviews());
        if (!cleanedReviews.isBlank()) {
            textBuilder.append("Müşteri Yorumları: ").append(cleanedReviews).append("\n");
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("db_id", String.valueOf(place.id()));
        metadata.put("place_id", place.placeId());
        metadata.put("name", place.name() != null ? place.name() : "");
        metadata.put("google_rating", place.googleRating() != null ? place.googleRating() : 0.0);
        metadata.put("tastymap_rating", place.tastymapRating() != null ? place.tastymapRating() : 0.0);
        metadata.put("types", typesJoined);
        metadata.put("is_open_now", Boolean.TRUE.equals(place.isOpenNow()));

        if (place.latitude() != null && place.longitude() != null) {
            metadata.put("latitude", place.latitude());
            metadata.put("longitude", place.longitude());
        }

        Map<String, Object> safeMetadata = new HashMap<>();
        metadata.forEach((key, val) -> {
            if (val instanceof Long l) {
                safeMetadata.put(key, l.intValue()); // Qdrant Long sevmez, Integer kabul eder
            } else {
                safeMetadata.put(key, val);
            }
        });

        return new Document(qdrantDocId, textBuilder.toString().trim(), safeMetadata);
    }

    private String calculateAverageScores(List<IndexedReviewDTO> reviews) {
        if (reviews == null || reviews.isEmpty()) return "";

        Map<ScoreType, List<Double>> scoreMap = new EnumMap<>(ScoreType.class);
        for (IndexedReviewDTO review : reviews) {
            if (review.scores() != null) {
                for (IndexedScoreDTO score : review.scores()) {
                    if (score != null && score.type() != null) {
                        scoreMap.computeIfAbsent(score.type(), k -> new ArrayList<>()).add(score.score());
                    }
                }
            }
        }

        if (scoreMap.isEmpty()) return "";

        return scoreMap.entrySet().stream()
                .map(entry -> {
                    double avg = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    return String.format("%s: %.1f/5", entry.getKey().name(), avg);
                })
                .collect(Collectors.joining(", "));
    }

    /**
     * TEKİL İNDEKSLEME: Tek bir mekan eklendiğinde veya yorum güncellendiğinde çalışır.
     */
    public void indexPlaceWithReviews(PlaceDetailsResult place, List<ReviewResult> reviews) {
        if (place == null || place.getPlace_id() == null || place.getPlace_id().isBlank()) {
            log.warn("Place ID bulunamadığı için Qdrant indekslemesi atlandı.");
            return;
        }

        Document doc = createDocument(place, reviews);
        vectorStore.add(List.of(doc));

        log.info("Restoran indeksi başarıyla güncellendi: {}", place.getPlace_id());
    }

    /**
     * TOPLU (BATCH) İNDEKSLEME: 100'erli paketler halinde Qdrant'a toplu veri gönderir.
     */
    public void indexPlacesBatch(List<Document> documents) {
        if (documents == null || documents.isEmpty()) return;

        try {
            List<String> docIds = documents.stream().map(Document::getId).toList();
            try {
                vectorStore.delete(docIds);
            } catch (Exception e) {
                log.debug("Eski indeksler silinirken uyarı alındı: {}", e.getMessage());
            }

            // TEK BİR AĞ İSTEĞİYLE TOPLU EKLEME
            vectorStore.add(documents);
            log.info("{} adet mekan topluca Qdrant'a eklendi.", documents.size());
        } catch (Exception e) {
            log.error("Batch indeksleme sırasında hata oluştu: {}", e.getMessage(), e);
        }
    }

    /**
     * YARDIMCI METOD: PlaceDetailsResult nesnesini Qdrant Document nesnesine çevirir.
     */
    public Document createDocument(PlaceDetailsResult place, List<ReviewResult> reviews) {
        String originalPlaceId = place.getPlace_id();

        // 1. Qdrant için geçerli deterministik UUID
        String qdrantDocId = UUID.nameUUIDFromBytes(originalPlaceId.getBytes(StandardCharsets.UTF_8)).toString();

        String typesText = (place.getTypes() != null && !place.getTypes().isEmpty())
                ? String.join(", ", place.getTypes())
                : "Belirtilmemiş";

        String reviewsCombined = extractCleanReviews(reviews);

        StringBuilder textBuilder = new StringBuilder();
        textBuilder.append("Restoran: ").append(place.getName()).append("\n");
        textBuilder.append("Kategoriler: ").append(typesText).append("\n");
        if (place.getFormattedAddress() != null) {
            textBuilder.append("Adres: ").append(place.getFormattedAddress()).append("\n");
        }
        if (!reviewsCombined.isBlank()) {
            textBuilder.append("Müşteri Yorumları ve Öne Çıkanlar: ").append(reviewsCombined).append("\n");
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("place_id", originalPlaceId);
        metadata.put("name", place.getName() != null ? place.getName() : "");
        metadata.put("google_rating", place.getGoogleRating() != null ? place.getGoogleRating() : 0.0);
        metadata.put("tastymap_rating", place.getTastyMapRating() != null ? place.getTastyMapRating() : 0.0);

        // List yerine düz String veriyoruz (M1 sürümü List kabul etmez)
        metadata.put("types", typesText);

        if (place.getOpeningHours() != null) {
            metadata.put("is_open_now", Boolean.TRUE.equals(place.getOpeningHours().getOpen_now()));
        } else {
            metadata.put("is_open_now", false);
        }

        // Koordinatları düz Double olarak ekliyoruz (Nested map koymuyoruz)
        if (place.getGeometry() != null && place.getGeometry().getLocation() != null) {
            metadata.put("latitude", place.getGeometry().getLocation().getLat());
            metadata.put("longitude", place.getGeometry().getLocation().getLng());
        }

        return new Document(qdrantDocId, textBuilder.toString().trim(), metadata);
    }

    private String extractCleanReviews(List<ReviewResult> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return "";
        }

        return reviews.stream()
                .map(ReviewResult::content)
                .filter(c -> c != null && !c.isBlank())
                .limit(MAX_REVIEWS_FOR_EMBEDDING)
                .map(c -> c.length() > MAX_REVIEW_CHAR_LENGTH ? c.substring(0, MAX_REVIEW_CHAR_LENGTH) + "..." : c)
                .map(c -> c.replaceAll("\\s+", " ").trim())
                .collect(Collectors.joining(" | "));
    }



    private String extractCleanReviewsFromDto(List<IndexedReviewDTO> reviews) {
        if (reviews == null || reviews.isEmpty()) return "";

        return reviews.stream()
                .map(IndexedReviewDTO::text)
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .limit(MAX_REVIEWS_FOR_EMBEDDING)
                .map(t -> t.length() > MAX_REVIEW_CHAR_LENGTH ? t.substring(0, MAX_REVIEW_CHAR_LENGTH) + "..." : t)
                .map(t -> t.replaceAll("\\s+", " ").trim())
                .collect(Collectors.joining(" | "));
    }

    private String extractCleanReviewsFromDtoV2(List<IndexedReviewDTO> reviews) {
        if (reviews == null || reviews.isEmpty()) return "";

        return reviews.stream()
                .map(IndexedReviewDTO::text)
                .filter(t -> t != null && t.trim().length() >= 25) // Anlamsız "güzeldi", "harika" gibi 1 kelimelik yorumları at
                .distinct()
                .limit(6) // 10 yerine en kaliteli ilk 6 yorum (VRAM ve hızı korur)
                .map(t -> {
                    String clean = t.replaceAll("\\s+", " ").trim();
                    if (clean.length() > 300) {
                        int lastSpace = clean.substring(0, 300).lastIndexOf(' ');
                        return (lastSpace > 200 ? clean.substring(0, lastSpace) : clean.substring(0, 300)) + "...";
                    }
                    return clean;
                })
                .collect(Collectors.joining(" | "));
    }



    private PlaceSummaryExtraction extractInsightsWithLLM(String placeName, String rawReviews) {
        if (rawReviews == null || rawReviews.isBlank()) {
            return null;
        }

        String systemText = """
        Sen bir mekan analiz motorusun. Sana verilen mekan adı ve müşteri yorumlarını incele.
        SADECE yorumlarda açıkça geçen gerçekleri ayıkla ve JSON üret.
        
        KESİN KURALLAR:
        1. Yorumlarda doğrudan bahsedilmeyen hiçbir bilgiyi ekleme (uydurma yasaktır).
        2. Yorumda geçmeyen alanları boş dizi [] olarak bırak.
        3. Özellikleri kısa, net kelime öbekleri halinde yaz.
        4. Yanıtın SADECE geçerli bir JSON objesi olsun. Başına veya sonuna hiçbir açıklama, selamlama veya markdown etiketi koyma.
        
        ÖRNEK JSON ŞEMASI:
        {
          "highlighted_foods": ["yemek1", "içecek1"],
          "atmosphere": ["sessiz", "loş"],
          "service_notes": ["hızlı servis"],
          "cautions": ["sıra bekleniyor"]
        }
        """;

        String userText = "Mekan: " + placeName + "\n" +
                "Müşteri Yorumları:\n" + rawReviews;

        try {
            // SystemMessage ve UserMessage doğrudan verildiğinde StringTemplate (ST4) devre dışı kalır!
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(systemText),
                    new UserMessage(userText)
            ));

            String rawResponse = chatClient.prompt(prompt)
                    .call()
                    .content();

            log.info("\n[LLM RAW RESPONSE - {}]:\n{}", placeName, rawResponse);

            if (rawResponse == null || rawResponse.isBlank()) {
                return null;
            }

            // Model bazen ```json ... ``` ile sarabilir, temizleyip Jackson'a veriyoruz
            String cleanJson = rawResponse.replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(cleanJson, PlaceSummaryExtraction.class);

        } catch (Exception e) {
            log.error("[LLM HATA - {}]: {}", placeName, e.getMessage());
            return null;
        }
    }


    private Document buildDocumentFromDtoV2(IndexedPlaceDTO place, Set<String> types) {
        String qdrantDocId = UUID.nameUUIDFromBytes(place.placeId().getBytes(StandardCharsets.UTF_8)).toString();

        // 1. Google gürültü tiplerini ayıkla
        List<String> meaningfulTypes = filterMeaningfulTypes(types);
        String rawReviews = extractCleanReviewsFromDtoV2(place.reviews());

        // 2. Yorum varsa LLM ile çıkarım yap
        PlaceSummaryExtraction insights = extractInsightsWithLLM(place.name(), rawReviews);

        // 3. BGE-M3 için anlamsal metin (Content) inşası
        StringBuilder semanticText = new StringBuilder();
        semanticText.append("Mekan: ").append(place.name()).append("\n");

        if (!meaningfulTypes.isEmpty()) {
            semanticText.append("Kategori: ").append(String.join(", ", meaningfulTypes)).append("\n");
        }

        if (insights != null) {
            if (insights.highlightedFoods() != null && !insights.highlightedFoods().isEmpty()) {
                semanticText.append("Öne Çıkan Lezzetler: ").append(String.join(", ", insights.highlightedFoods())).append("\n");
            }
            if (insights.atmosphere() != null && !insights.atmosphere().isEmpty()) {
                semanticText.append("Ortam ve Ambiyans: ").append(String.join(", ", insights.atmosphere())).append("\n");
            }
            if (insights.serviceNotes() != null && !insights.serviceNotes().isEmpty()) {
                semanticText.append("Hizmet ve Deneyim: ").append(String.join(", ", insights.serviceNotes())).append("\n");
            }
            if (insights.cautions() != null && !insights.cautions().isEmpty()) {
                semanticText.append("Dikkat Edilmesi Gerekenler: ").append(String.join(", ", insights.cautions())).append("\n");
            }
        } else if (!rawReviews.isBlank()) {
            // LLM hata verirse veya boş dönerse fallback
            semanticText.append("Yorum Notları: ").append(rawReviews).append("\n");
        }

        // 4. Qdrant Payload (Metadata)
        RestaurantMetadata metadata = new RestaurantMetadata();
        metadata.setDbId(place.id());
        metadata.setPlaceId(place.placeId());
        metadata.setName(place.name());
        metadata.setGoogleRating(place.googleRating());
        metadata.setTastymapRating(place.tastymapRating());
        metadata.setTypes(meaningfulTypes);
        metadata.setLatitude(place.latitude());
        metadata.setLongitude(place.longitude());

        return new Document(qdrantDocId, semanticText.toString().trim(), metadata.toMap());
    }

    private List<String> filterMeaningfulTypes(Set<String> types) {
        if (types == null || types.isEmpty()) return Collections.emptyList();
        Set<String> ignored = Set.of("establishment", "point_of_interest", "food", "store");
        return types.stream()
                .filter(t -> !ignored.contains(t.toLowerCase()))
                .toList();
    }

    /**
     * Qdrant'a gönderilen dökümanların anlamsal metin (content)
     * içeriğini çerçeveli ve okunaklı biçimde loglar.
     */
    private void logGeneratedContent(Document doc) {
        if (doc == null || doc.getContent() == null) {
            log.warn("Loglanacak Document veya Content içeriği boş.");
            return;
        }

        String border = "=".repeat(60);
        String placeId = doc.getMetadata() != null ? String.valueOf(doc.getMetadata().get("place_id")) : "Bilinmiyor";

        log.info("\n{}\n[QDRANT INDEX CONTENT] -> Place ID: {}\n{}\n{}\n{}",
                border,
                placeId,
                border,
                doc.getContent(),
                border);
    }
}