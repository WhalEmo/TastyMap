package com.beem.TastyMap.user.account.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaUserConfig {

    public static final String USER_LIFECYCLE_EVENTS_TOPIC = "user-lifecycle-events";
    public static final String USER_COUNTER_ADJUSTMENT_COMMANDS_TOPIC = "user-counter-decrement-commands";

    @Bean
    public NewTopic userLifecycleEventsTopic() {
        return TopicBuilder.name(USER_LIFECYCLE_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userCounterDecrementCommandsTopic() {
        return TopicBuilder.name(USER_COUNTER_ADJUSTMENT_COMMANDS_TOPIC)
                .partitions(5)
                .replicas(1)
                .build();
    }
}
