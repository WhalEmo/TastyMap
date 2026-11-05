package com.beem.TastyMap.rag.service;

import com.beem.TastyMap.maps.data.PlaceDetailsResult;
import com.beem.TastyMap.mapsReview.data.ReviewResult;
import com.beem.TastyMap.mapsReview.enums.ScoreType;
import com.beem.TastyMap.rag.component.QdrantDocumentMapper;
import com.beem.TastyMap.rag.data.document.RestaurantDocument;
import com.beem.TastyMap.rag.data.document.RestaurantMetadata;
import com.beem.TastyMap.rag.data.document.VenueCategory;
import com.beem.TastyMap.rag.data.dto.IndexedPlaceDTO;
import com.beem.TastyMap.rag.data.dto.IndexedReviewDTO;
import com.beem.TastyMap.rag.data.dto.IndexedScoreDTO;
import com.beem.TastyMap.rag.data.dto.PlaceSummaryExtraction;
import com.beem.TastyMap.rag.data.enums.AiProvider;
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

    private final VectorStore vectorStore;
    private final PlaceIndexingQueryRepository placeIndexingQueryRepository;
    private final AiProviderFactory aiProviderFactory;
    private final QdrantDocumentMapper qdrantDocumentMapper;

    private static final int CHUNK_SIZE = 100;
    private static final int MAX_REVIEWS_FOR_EMBEDDING = 10;
    private static final int MAX_REVIEW_CHAR_LENGTH = 300;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();


    public RestaurantIndexingService(
            VectorStore vectorStore,
            PlaceIndexingQueryRepository placeIndexingQueryRepository,
            AiProviderFactory aiProviderFactory,
            QdrantDocumentMapper qdrantDocumentMapper
    ) {
        this.vectorStore = vectorStore;
        this.placeIndexingQueryRepository = placeIndexingQueryRepository;
        this.aiProviderFactory = aiProviderFactory;
        this.qdrantDocumentMapper = qdrantDocumentMapper;
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
                int writtenCount = safeIndexPlacesBatch(documents);
                totalProcessed += writtenCount;
                log.info("Paket #{} tamamlandı. (İşlenen: {}, Son DB ID: {})",
                        chunkNumber, totalProcessed, chunk.get(chunk.size() - 1).id());
            }

            lastSeenId = chunk.get(chunk.size() - 1).id();
            chunkNumber++;
        }
        log.info("Toplu indeksleme başarıyla bitti! Qdrant'a aktarılan toplam mekan sayısı: {}", totalProcessed);
    }

    private int safeIndexPlacesBatch(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return 0;
        }
        try {
            indexPlacesBatch(documents);
            return documents.size();
        } catch (Exception batchEx) {
            log.warn("⚠️ Paket toplu yazılamadı ({} döküman). Tekil kurtarma (Fallback) moduna geçiliyor... Hata Özeti: {}",
                    documents.size(), batchEx.getMessage());

            int successCount = 0;
            List<String> failedPlaces = new ArrayList<>();

            for (Document doc : documents) {
                try {
                    indexPlacesBatch(List.of(doc));
                    successCount++;
                } catch (Exception singleEx) {
                    String placeName = Optional.ofNullable(doc.getMetadata().get("name"))
                            .map(String::valueOf).orElse("Bilinmeyen Mekan");
                    String placeId = Optional.ofNullable(doc.getMetadata().get("place_id"))
                            .map(String::valueOf).orElse(doc.getId());
                    String dbId = Optional.ofNullable(doc.getMetadata().get("db_id"))
                            .map(String::valueOf).orElse("-");

                    log.error("❌ [ZEHİRLİ DÖKÜMAN ATLANDI] -> Ad: '{}', PlaceId: '{}', DB ID: '{}', Hata: {}",
                            placeName, placeId, dbId, singleEx.getMessage());

                    failedPlaces.add(String.format("'%s' (ID: %s)", placeName, dbId));
                }
            }

            log.info("🔄 Kurtarma Tamamlandı: {}/{} döküman başarıyla yazıldı. Atlananlar ({} adet): {}",
                    successCount, documents.size(), failedPlaces.size(), failedPlaces);

            return successCount;
        }
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


    private PlaceSummaryExtraction extractInsightsWithLLM(String placeName, List<String> types, String rawReviews) {
        String reviewsText = (rawReviews != null && !rawReviews.isBlank())
                ? rawReviews
                : "Ayrıntılı müşteri yorumu bulunmuyor. Mekan adı ve türünden çıkarım yap.";

        String typesText = (types != null && !types.isEmpty())
                ? String.join(", ", types)
                : "Belirtilmemiş";

        String systemText = """
        Sen bir gastronomi ve mekan analiz motorusun. Mekan adı ve müşteri yorumlarını inceleyip yapılandırılmış JSON üret.
        
        KATEGORİ SEÇENEKLERİ (venue_category için sadece bu değerlerden birini seç):
        - CAFE: Kahve odaklı mekanlar, 3. nesil kahveciler, çay evleri.
        - RESTAURANT: Genel yemek, ev yemekleri, dünya mutfağı.
        - MEAT_AND_GRILL: Döner, kebap, etliekmek, tantuni, köfte, ocakbaşı, ciğerci/ciğer, lahmacun/pide, dürüm, sakatat, çorbacı, ızgara et/tavuk. (DİKKAT: Mekan adında veya lezzetlerinde ciğer, lahmacun, dürüm, köfte, kebap geçiyorsa RESTAURANT DEĞİL, kesinlikle MEAT_AND_GRILL seçilmelidir).
        - BAKERY_AND_DESSERT: Pastane, fırın, unlu mamulleri, börekçi, tatlıcı, waffle, dondurmacı. (KURAL: Mekan adında unlu mamul, pastane, fırın, börek geçiyorsa BAKERY_AND_DESSERT seçilmelidir).
        - SEAFOOD: Balık ve deniz ürünleri restoranları.
        - FAST_FOOD: Burger, pizza, çiğköfte, sokak lezzetleri.
        - NON_DINING: İnternet kafe, espor/oyun merkezi, playstation salonu, bilardo/okey salonu, kıraathane, çocuk parkı, kurs/akademi.

        KRİTİK KURALLAR:
        1. is_dining: Mekan NON_DINING (oyun salonu, espor, okey salonu ve benzeri) ise içinde tost/içecek satılsa dahi KESİNLİKLE FALSE olmalıdır. Restoran, kafe, pastane, tatlıcı ve fırınlar için KESİNLİKLE TRUE olmalıdır.
        2. is_vegan_friendly: Yorumlarda vegan ürün/tatlı VEYA bitkisel süt alternatifleri (yulaf sütü, soya sütü, badem sütü, hindistan cevizi sütü) açıkça geçiyorsa TRUE, aksi halde FALSE.
        3. is_vegetarian_friendly: Mekanda etsiz doyurucu yemek alternatifleri, zengin meze seçenekleri varsa VEYA mekan vegan ise TRUE. Saf kebapçı/ciğercilerde belirgin vejetaryen meze/yemek vurgusu yoksa FALSE.
        4. is_gluten_free_friendly: Yorumlarda açıkça 'glutensiz' veya 'çölyak' vurgusu varsa TRUE, aksi halde FALSE.
        5. dietary_tags: Yorumlarda açıkça geçen beslenme etiketleri (Örn: ["yulaf sütü", "glutensiz ekmek", "vegan lazanya"]). Yoksa boş liste [].
        6. highlighted_foods, atmosphere, service_notes, cautions: Yorumlardan çıkarılan özet maddeler. Yorumda açıkça geçmeyen hiçbir lezzeti uydurma. Yoksa boş liste [].
        7. Yorumda açıkça geçmeyen hiçbir şeyi uydurma.
        8. SADECE geçerli bir JSON objesi döndür. Markdown etiketi (```json) kullanma.

        ÖRNEK ÇIKTI FORMATI:
        {
          "is_dining": true,
          "venue_category": "CAFE",
          "is_vegan_friendly": true,
          "is_vegetarian_friendly": true,
          "is_gluten_free_friendly": false,
          "dietary_tags": ["yulaf sütü", "bitkisel süt"],
          "highlighted_foods": ["filtre kahve", "cheesecake"],
          "atmosphere": ["sakin", "çalışmaya uygun"],
          "service_notes": ["güler yüzlü personel"],
          "cautions": ["priz az"]
        }
        """;

        String userText = "Mekan Adı: " + placeName + "\n" +
                "Mekan Türü/Kategori: " + typesText + "\n" +
                "Müşteri Yorumları:\n" + reviewsText;

        try {
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(systemText),
                    new UserMessage(userText)
            ));

            ChatClient chatClient = aiProviderFactory.getClient(AiProvider.OLLAMA_INDEXER);

            String rawResponse = chatClient.prompt(prompt)
                    .call()
                    .content();

            log.info("\n[LLM RAW RESPONSE - {}]:\n{}", placeName, rawResponse);

            if (rawResponse == null || rawResponse.isBlank()) {
                return null;
            }

            String cleanJson = rawResponse.trim();
            int firstBrace = cleanJson.indexOf('{');
            int lastBrace = cleanJson.lastIndexOf('}');
            if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                cleanJson = cleanJson.substring(firstBrace, lastBrace + 1);
            }

            return OBJECT_MAPPER.readValue(cleanJson, PlaceSummaryExtraction.class);

        } catch (Exception e) {
            log.error("[LLM HATA - {}]: {}", placeName, e.getMessage());
            return null;
        }
    }


    private Document buildDocumentFromDtoV2(IndexedPlaceDTO place, Set<String> types) {
        String qdrantDocId = UUID.nameUUIDFromBytes(place.placeId().getBytes(StandardCharsets.UTF_8)).toString();

        List<String> meaningfulTypes = filterMeaningfulTypes(types);
        String rawReviews = extractCleanReviewsFromDtoV2(place.reviews());

        PlaceSummaryExtraction insights = extractInsightsWithLLM(place.name(), meaningfulTypes, rawReviews);

        // 1. BGE-M3 Anlamsal Metin
        StringBuilder semanticText = new StringBuilder();
        semanticText.append("Mekan: ").append(place.name()).append("\n");

        if (!meaningfulTypes.isEmpty()) {
            semanticText.append("Kategori: ").append(String.join(", ", meaningfulTypes)).append("\n");
        }

        if (insights != null) {
            if (insights.venueCategory() != null) {
                semanticText.append("İşletme Türü: ").append(insights.venueCategory()).append("\n");
            }
            if (insights.dietaryTags() != null && !insights.dietaryTags().isEmpty()) {
                semanticText.append("Diyet ve Beslenme Alternatifleri: ")
                        .append(String.join(", ", insights.dietaryTags())).append("\n");
            }
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
            semanticText.append("Yorum Notları: ").append(rawReviews).append("\n");
        }

        // 2. Qdrant Payload (Metadata)
        RestaurantMetadata metadata = new RestaurantMetadata();
        metadata.setDbId(place.id());
        metadata.setPlaceId(place.placeId());
        metadata.setName(place.name());
        metadata.setGoogleRating(place.googleRating());
        metadata.setTastymapRating(place.tastymapRating());
        metadata.setTypes(meaningfulTypes);
        metadata.setLatitude(place.latitude());
        metadata.setLongitude(place.longitude());

        if (insights != null) {
            metadata.setVenueCategoryFromString(insights.venueCategory());

            if (metadata.getVenueCategory() == VenueCategory.NON_DINING) {
                metadata.setDining(false);
            } else {
                metadata.setDining(insights.safeIsDining());
            }

            if (insights.safeIsVeganFriendly()) {
                metadata.setVegetarianFriendly(true);
            } else {
                metadata.setVegetarianFriendly(insights.safeIsVegetarianFriendly());
            }

            metadata.setVeganFriendly(insights.safeIsVeganFriendly());
            metadata.setGlutenFreeFriendly(insights.safeIsGlutenFreeFriendly());
            if (insights.dietaryTags() != null) {
                metadata.setDietaryTags(insights.dietaryTags());
            }

            if (insights.highlightedFoods() != null) {
                metadata.setHighlightedFoods(insights.highlightedFoods());
            }
            if (insights.atmosphere() != null) {
                metadata.setAtmosphere(insights.atmosphere());
            }
            if (insights.serviceNotes() != null) {
                metadata.setServiceNotes(insights.serviceNotes());
            }
            if (insights.cautions() != null) {
                metadata.setCautions(insights.cautions());
            }
        }

        log.info("MetaData: {}", metadata);

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

        RestaurantMetadata metadata = qdrantDocumentMapper.fromMetadataMap(doc.getMetadata());

        String border = "=".repeat(75);
        String subBorder = "-".repeat(75);

        String locationStr = (metadata.getLatitude() != null && metadata.getLongitude() != null)
                ? String.format("Lat: %.6f, Lon: %.6f", metadata.getLatitude(), metadata.getLongitude())
                : "Yok";

        log.info("\n{}\n" +
                        "📍 [QDRANT INDEX DOC] -> {} (DB ID: {}, Place ID: {})\n" +
                        "{}\n" +
                        "🏷️  Kategori: [{}] | Yeme-İçme Mi: [{}]\n" +
                        "🥗 Diyet Bayrakları -> Vegan: [{}] | Vejetaryen: [{}] | Glutensiz: [{}]\n" +
                        "🏷️  Diyet Etiketleri -> {}\n" +
                        "⭐ Puan: {} | Konum: [{}]\n" +
                        "{}\n" +
                        "📄 [BGE-M3 ANLAMSAL İÇERİK]:\n{}\n" +
                        "{}",
                border,
                metadata.getName(), metadata.getDbId(), metadata.getPlaceId(),
                subBorder,
                metadata.getVenueCategory() != null ? metadata.getVenueCategory().getKey() : "BELİRTİLMEMİŞ",
                Boolean.TRUE.equals(metadata.getDining()) ? "EVET" : "HAYIR (NON_DINING)",
                Boolean.TRUE.equals(metadata.getVeganFriendly()) ? "EVET" : "HAYIR",
                Boolean.TRUE.equals(metadata.getVegetarianFriendly()) ? "EVET" : "HAYIR",
                Boolean.TRUE.equals(metadata.getGlutenFreeFriendly()) ? "EVET" : "HAYIR",
                metadata.getDietaryTags() != null ? metadata.getDietaryTags() : Collections.emptyList(),
                metadata.getGoogleRating(), locationStr,
                subBorder,
                doc.getContent(),
                border
        );
    }

    public void testIndexSamplePlaces(Long lastSeenId, int limit) {
        log.info("🧪 Test İndeksleme Başlatıldı (Limit: {})...", limit);

        List<IndexedPlaceDTO> sample = placeIndexingQueryRepository.fetchNextChunk(lastSeenId, limit);
        if (sample == null || sample.isEmpty()) {
            log.warn("Test için veritabanında mekan bulunamadı!");
            return;
        }

        List<Long> chunkIds = sample.stream().map(IndexedPlaceDTO::id).toList();
        Map<Long, Set<String>> typesMap = placeIndexingQueryRepository.fetchTypesForPlaceIds(chunkIds);

        for (IndexedPlaceDTO dto : sample) {
            try {
                Set<String> types = typesMap.getOrDefault(dto.id(), Collections.emptySet());
                Document doc = buildDocumentFromDtoV2(dto, types);
                logGeneratedContent(doc);
                // Test aşamasında Qdrant'a yazmak istersen:
                // vectorStore.add(List.of(doc));
            } catch (Exception e) {
                log.error("Test sırasında hata (PlaceId: {}): {}", dto.placeId(), e.getMessage(), e);
            }
        }
        log.info("🧪 Test İndeksleme Tamamlandı!");
    }


    public void runSyntheticExtractionTests() {
        log.info("🧪 Sentetik Zorlu Senaryo Testleri Başlatılıyor...");

        record TestCase(String name, List<String> types, String reviews, String expectation) {}

        List<TestCase> cases = List.of(
                new TestCase(
                        "Ateş Ocakbaşı & Meze Evi",
                        List.of("restaurant"),
                        "Kebapları ve pirzolası harikaydı. Eşim et yemiyor ama onun için közde patlıcan, gavurdağı ve güveçte mantar söyledik, mezeleri çok zengin ve doyurucuydu.",
                        "Beklenti: MEAT_AND_GRILL, is_dining: true, vegetarian: true, vegan: false"
                ),
                new TestCase(
                        "Botanica Vegan Bistro",
                        List.of("restaurant", "food"),
                        "Menüdeki her şey tamamen bitkisel. Soya kıymalı lazanyası ve kaju kremalı vegan cheesecake'i muhteşemdi. Hayvansal hiçbir ürün kullanmıyorlar.",
                        "Beklenti: RESTAURANT, is_dining: true, vegan: true, vegetarian: true, tag: [vegan cheesecake]"
                ),
                new TestCase(
                        "Nordic Roast Coffee Lab",
                        List.of("cafe"),
                        "V60 demlemeleri çok başarılı. Kahvemi yulaf sütü ile aldım, yanında da unsuz ve glutensiz fındıklı brownie denedim, çölyak olanlar için ayrı kapalı kutuda sunuyorlar.",
                        "Beklenti: CAFE, is_dining: true, vegan: true, gluten: true, tags: [yulaf sütü, glutensiz brownie]"
                ),
                new TestCase(
                        "Hacı Bey İşkembe & Kelle Paça Salonu",
                        List.of("restaurant"),
                        "Kelle paça ve ayak paçası Konya'nın en iyisi. Arkadaşım mercimek çorbası içti ama çorbalar et suyuyla yapıldığı için et yemeyenlere göre değil.",
                        "Beklenti: MEAT_AND_GRILL, is_dining: true, vegan: false, vegetarian: false"
                ),
                new TestCase(
                        "Matrix Espor & Playstation Cafe",
                        List.of("point_of_interest"),
                        "RTX 4080 sistemler ve PS5 kolları sıfır gibiydi. Oyun oynarken karışık tost ve kola söyledik, hızlı getirdiler. 5 saat oyun attık.",
                        "Beklenti: NON_DINING, is_dining: false"
                ),
                new TestCase(
                        "Glutensiz Yaşam Fırın & Cafe",
                        List.of("bakery"),
                        "Mutfaklarına kesinlikle buğday unu girmiyor, çapraz bulaşma riski sıfır. Glutensiz ekmekleri ve çölyak dostu simitleri taptazeydi.",
                        "Beklenti: BAKERY_AND_DESSERT, is_dining: true, gluten: true, tags: [glutensiz ekmek, çölyak dostu]"
                )
        );

        for (TestCase tc : cases) {
            log.info("\n--------------------------------------------------");
            log.info("🧪 TEST: {}", tc.name());
            log.info("🎯 {}", tc.expectation());

            PlaceSummaryExtraction result = extractInsightsWithLLM(tc.name(), tc.types(), tc.reviews());

            if (result != null) {
                log.info("Sonuç -> Kategori: [{}], Dining: [{}], Vegan: [{}], Vejetaryen: [{}], Glutensiz: [{}], Etiketler: {}",
                        result.venueCategory(), result.isDining(), result.isVeganFriendly(),
                        result.isVegetarianFriendly(), result.isGlutenFreeFriendly(), result.dietaryTags());
            } else {
                log.error("❌ Model boş veya hatalı yanıt döndü!");
            }
        }
        log.info("\n🧪 Sentetik Testler Tamamlandı!");
    }


}