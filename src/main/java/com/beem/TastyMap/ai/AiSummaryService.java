package com.beem.TastyMap.ai;

import com.beem.TastyMap.mapsReview.entity.ReviewEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class AiSummaryService {

    private final RestTemplate restTemplate;

    public AiSummaryService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${spring.ai.ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${spring.ai.ollama.comment.options.model}")
    private String modelName;

    public AiSummaryResponse getRestaurantSummary(List<ReviewEntity> reviews) {
        String combinedComments = combineComments(reviews);

        if (combinedComments == null) {
            return new AiSummaryResponse(false, null);
        }


        String fullUrl = ollamaUrl + "/api/generate";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("prompt", combinedComments);
        requestBody.put("stream", false);

        Map<String, Object> options = new HashMap<>();
        options.put("temperature", 0.1);
        options.put("repetition_penalty", 1.15);
        requestBody.put("options", options);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> requestEntity =
                new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<OllamaResponse> response = restTemplate.postForEntity(fullUrl, requestEntity, OllamaResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String aiResult = response.getBody().response();
                return new AiSummaryResponse(true, aiResult);
            }
        } catch (RestClientException e) {
            log.error("Ollama API iletişim hatası: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Yapay zeka özeti oluşturulurken beklenmeyen bir hata oluştu.", e);
        }

        return new AiSummaryResponse(false, null);
    }

    private String combineComments(List<ReviewEntity> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        for (ReviewEntity review : reviews) {
            String text = review.getText();
            if (text == null || text.trim().isEmpty()) {
                continue;
            }

            String scoreStr = "?";
            if (review.getRating() != null) {
                double rating = review.getRating();
                if (rating % 1 == 0) {
                    scoreStr = String.format(Locale.US, "%.0f", rating);
                } else {
                    scoreStr = String.valueOf(rating);
                }
            }

            sb.append("- [Puan: ")
                    .append(scoreStr)
                    .append("/5]: ")
                    .append(text.trim())
                    .append("\n");
        }

        if (sb.length() == 0) {
            return null;
        }

        return sb.toString();
    }
}