package com.beem.TastyMap.rag.config;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorStoreConfig {
    @Bean
    public ChatClient chatClient(OllamaChatClient ollamaChatClient) {
        return ollamaChatClient;
    }
}