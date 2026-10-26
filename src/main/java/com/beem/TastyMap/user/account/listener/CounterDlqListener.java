package com.beem.TastyMap.user.account.listener;

import com.beem.TastyMap.user.account.event.AdjustCounterBatchCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CounterDlqListener {

    private static final Logger log = LoggerFactory.getLogger(CounterDlqListener.class);

    @KafkaListener(
            topics = "#{T(com.beem.TastyMap.user.account.config.KafkaUserConfig).USER_COUNTER_ADJUSTMENT_COMMANDS_TOPIC + '-dlq'}",
            groupId = "counter-dlq-handler-group"
    )
    public void handleDlqBatch(AdjustCounterBatchCommand command) {
        log.error("CRITICAL ERROR: Counter adjustment batch permanently failed! TargetUser: {}, Field: {}, Action: {}, Batch Size: {}",
                command.getTargetUserId(),
                command.getTargetField(),
                command.getAction(),
                command.getUserIdsToUpdate().size());
    }
}