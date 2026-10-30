package com.beem.TastyMap.rag.service;

import com.beem.TastyMap.redis.RedisCacheService;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class CachedEmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final RedisCacheService redisCacheService;

    private static final Locale TR = Locale.forLanguageTag("tr");
    private static final String CACHE_PREFIX = "embedding:v1:";
    private static final long CACHE_TTL_SECONDS = 7 * 24 * 60 * 60; // 7 Gün (Saniye cinsinden)
    private static final TypeReference<List<Float>> VECTOR_TYPE = new TypeReference<>() {};
    private static final Logger log = LoggerFactory.getLogger(CachedEmbeddingService.class);

    public CachedEmbeddingService(EmbeddingModel embeddingModel, RedisCacheService redisCacheService) {
        this.embeddingModel = embeddingModel;
        this.redisCacheService = redisCacheService;
    }

    public List<Float> getOrComputeEmbedding(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String normalizedQuery = normalize(query);
        String cacheKey = CACHE_PREFIX + hashKey(normalizedQuery);

        List<Float> cachedVector = redisCacheService.get(cacheKey, VECTOR_TYPE);
        if (cachedVector != null && !cachedVector.isEmpty()) {
            log.info("⚡ [EmbeddingCache] HIT -> Sorgu: '{}' (Key: {})", normalizedQuery, cacheKey);
            return cachedVector;
        }

        // 2. Cache MISS -> Ollama BGE-M3 çağrısı
        log.info("🐢 [EmbeddingCache] MISS -> BGE-M3 hesaplaması başlatılıyor: '{}'", normalizedQuery);
        long start = System.currentTimeMillis();

        float[] rawFloats = embeddingModel.embed(normalizedQuery);
        long elapsed = System.currentTimeMillis() - start;

        List<Float> vectorList = new ArrayList<>(rawFloats.length);
        for (float f : rawFloats) {
            vectorList.add(f);
        }

        log.info("⏱️ [EmbeddingCache] BGE-M3 tamamlandı: {} ms | Boyut: {}", elapsed, vectorList.size());

        if (redisCacheService.setQuietly(cacheKey, vectorList, CACHE_TTL_SECONDS)) {
            log.debug("💾 [EmbeddingCache] Vektör başarıyla önbelleklendi: {}", cacheKey);
        } else {
            log.warn("⚠️ [EmbeddingCache] Redis'e yazılamadı ancak arama akışı devam ediyor (Key: {})", cacheKey);
        }

        return vectorList;
    }

    private String normalize(String text) {
        return text.trim()
                .toLowerCase(Locale.forLanguageTag("tr-TR"))
                .replaceAll("\\s+", " ");
    }

    private String hashKey(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(text.hashCode());
        }
    }

}
