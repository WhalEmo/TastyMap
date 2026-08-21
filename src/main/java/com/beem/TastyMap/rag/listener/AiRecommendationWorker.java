package com.beem.TastyMap.rag.listener;

import com.beem.TastyMap.rag.config.KafkaConfig;
import com.beem.TastyMap.rag.data.event.UserQueryEvent;
import com.beem.TastyMap.rag.service.RagPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiRecommendationWorker {

    private final VectorStore vectorStore;
    private final ChatClient.Builder chatClientBuilder;
    private final RagPromptBuilder ragPromptBuilder;

    @KafkaListener(topics = KafkaConfig.RESTAURANT_AI_REQUESTS_TOPIC, groupId = "tasty-ai-group")
    public void processAiRequest(UserQueryEvent event) {
        log.info("Kafka'dan yeni RAG isteği alındı. Kullanıcı ID: {}", event.getUserId());

        SearchRequest searchRequest = SearchRequest.query(event.getUserQuery()).withTopK(8);
        List<Document> similarDocs = vectorStore.similaritySearch(searchRequest);

        String systemPrompt = ragPromptBuilder.buildSystemPrompt(
                event.getUserId(),
                event.isIgnoreAllergies(),
                similarDocs,
                event.getUserQuery()
        );

        // 3. LLM Üretimi (Modern ChatClient API)
        String aiResponse = chatClientBuilder.build()
                .prompt()
                .user(systemPrompt)
                .call()
                .content();

        log.info("--- AI YANITI ÜRETİLDİ (Kullanıcı ID: {}) ---", event.getUserId());
        log.info(aiResponse);

        // TODO: Sonucu bildir (Notification/WebSocket)
    }
}