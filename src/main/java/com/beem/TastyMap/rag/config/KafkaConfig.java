package com.beem.TastyMap.rag.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String RESTAURANT_AI_REQUESTS_TOPIC = "restaurant-ai-requests";

    @Bean
    public NewTopic restaurantAiRequestsTopic() {
        return TopicBuilder.name(RESTAURANT_AI_REQUESTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}