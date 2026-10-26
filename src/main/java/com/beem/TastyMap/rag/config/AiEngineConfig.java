package com.beem.TastyMap.rag.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AiEngineConfig {

    // --- Qdrant Ayarları ---
    @Value("${spring.ai.vectorstore.qdrant.host:localhost}")
    private String qdrantHost;

    @Value("${spring.ai.vectorstore.qdrant.port:6334}")
    private int qdrantPort;

    @Value("${spring.ai.vectorstore.qdrant.collection-name:restaurants}")
    private String collectionName;

    // --- Ollama Ayarları ---
    @Value("${spring.ai.ollama.chat.options.model}")
    private String chatModelName;

    @Value("${spring.ai.ollama.embedding.options.model}")
    private String embeddingModel;

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    // --- Groq Ayarları ---
    @Value("${spring.ai.openai.api-key}")
    private String groqApiKey;

    @Value("${spring.ai.openai.base-url:https://api.groq.com/openai}")
    private String groqBaseUrl;

    @Value("${spring.ai.openai.chat.options.model:qwen/qwen3.8-27b}")
    private String groqChatModelName;

    @Value("${spring.ai.openai.chat.options.temperature:0.3}")
    private Float groqTemperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:512}")
    private Integer groqMaxTokens;

    // ========================================================
    // 1. OLLAMA MOTORU (Özel ObjectMapper & RestClient)
    // ========================================================
    @Bean
    public OllamaApi ollamaApi() {
        ObjectMapper ollamaMapper = new ObjectMapper();
        ollamaMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ollamaMapper.findAndRegisterModules();

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(ollamaMapper);

        RestClient.Builder customClientBuilder = RestClient.builder()
                .baseUrl(ollamaBaseUrl)
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(0, converter);
                });

        return new OllamaApi(ollamaBaseUrl, customClientBuilder);
    }

    @Bean
    @Primary
    public EmbeddingModel embeddingModel(OllamaApi ollamaApi) {
        return new OllamaEmbeddingModel(
                ollamaApi,
                OllamaOptions.create().withModel(embeddingModel)
        );
    }

    @Bean(name = "ollamaChatModel")
    @Primary
    public OllamaChatModel ollamaChatModel(OllamaApi ollamaApi) {
        return new OllamaChatModel(
                ollamaApi,
                optionsCreateV4()
        );
    }

    // ========================================================
    // 2. GROQ MOTORU (Groq'un x_groq alanlarını yutan RestClient)
    // ========================================================
    @Bean
    public OpenAiApi groqOpenAiApi() {
        ObjectMapper groqMapper = new ObjectMapper();
        // Groq'un x_groq, service_tier vb. alanlarında patlamaması için:
        groqMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        groqMapper.findAndRegisterModules();

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(groqMapper);

        RestClient.Builder customClientBuilder = RestClient.builder()
                .baseUrl(groqBaseUrl)
                .defaultHeader("Authorization", "Bearer " + groqApiKey)
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(0, converter);
                });

        // 4. parametre olarak WebClient.builder() ekliyoruz:
        return new OpenAiApi(groqBaseUrl, groqApiKey, customClientBuilder, WebClient.builder());
    }

    @Bean(name = "groqChatModel")
    public OpenAiChatModel groqChatModel(OpenAiApi groqOpenAiApi) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(groqChatModelName)
                .withTemperature(groqTemperature)
                .withMaxTokens(groqMaxTokens)
                .build();

        return new OpenAiChatModel(groqOpenAiApi, options);
    }

    // ========================================================
    // 3. CHAT CLIENT TANIMLARI (AiProviderFactory İçin)
    // ========================================================
    @Bean(name = "groqChatClient")
    public ChatClient groqChatClient(@Qualifier("groqChatModel") OpenAiChatModel groqChatModel) {
        return ChatClient.builder(groqChatModel).build();
    }

    @Bean(name = "ollamaChatClient")
    public ChatClient ollamaChatClient(@Qualifier("ollamaChatModel") OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel).build();
    }

    // ========================================================
    // 4. QDRANT & VECTOR STORE
    // ========================================================
    @Bean
    public QdrantClient qdrantClient() {
        return new QdrantClient(
                QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, false).build()
        );
    }

    @Bean
    public VectorStore vectorStore(QdrantClient qdrantClient, EmbeddingModel embeddingModel) {
        return new QdrantVectorStore(qdrantClient, collectionName, embeddingModel, true);
    }

    // ========================================================
    // 5. OLLAMA SEÇENEK PROFİLLERİ
    // ========================================================
    private OllamaOptions optionsCreateV1() {
        return OllamaOptions.create()
                .withModel(chatModelName)
                .withTemperature(0.2f)
                .withTopP(0.85f)
                .withRepeatPenalty(1.15f)
                .withNumCtx(2048)
                .withNumPredict(400);
    }

    private OllamaOptions optionsCreateV2() {
        return OllamaOptions.create()
                .withModel(chatModelName)
                .withTemperature(0.0f)
                .withTopP(0.7f)
                .withRepeatPenalty(1.25f)
                .withNumCtx(2048)
                .withNumPredict(200);
    }

    private OllamaOptions optionsCreateV3() {
        return OllamaOptions.create()
                .withModel(chatModelName)
                .withTemperature(0.15f)
                .withTopP(0.85f)
                .withRepeatPenalty(1.15f)
                .withNumCtx(2048)
                .withNumPredict(512);
    }

    private OllamaOptions optionsCreateV4() {
        return OllamaOptions.create()
                .withModel(chatModelName)
                .withTemperature(0.15f)
                .withTopP(0.85f)
                .withRepeatPenalty(1.15f)
                .withNumCtx(2048);
    }
}