package com.beem.TastyMap.user.account.listener;

import com.beem.TastyMap.user.account.config.KafkaUserConfig;
import com.beem.TastyMap.user.account.event.AdjustCounterBatchCommand;
import com.beem.TastyMap.user.account.event.UserLifecycleEvent;
import com.beem.TastyMap.user.account.event.UserLifecycleEventType;
import com.beem.TastyMap.user.subscribe.repo.SubscribeRepo;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserLifecycleEventListener {

    private final SubscribeRepo subscribeRepo;
    private final KafkaTemplate<String, AdjustCounterBatchCommand> kafkaTemplate;

    public UserLifecycleEventListener(SubscribeRepo subscribeRepo, KafkaTemplate<String, AdjustCounterBatchCommand> kafkaTemplate) {
        this.subscribeRepo = subscribeRepo;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(
            topics = "#{T(com.beem.TastyMap.user.account.config.KafkaUserConfig).USER_LIFECYCLE_EVENTS_TOPIC}",
            groupId = "user-relationships-group"
    )
    public void handleUserLifecycle(UserLifecycleEvent event) {
        Long targetUserId = event.getUserId();
        AdjustCounterBatchCommand.Action action = (event.getEventType() == UserLifecycleEventType.SOFT_DELETED)
                ? AdjustCounterBatchCommand.Action.DECREMENT
                : AdjustCounterBatchCommand.Action.INCREMENT;

        // 1. ADIM: Takipçilerin 'subscribedCount' (Takip Edilen) sayısını güncelle
        processChunk(targetUserId, action, AdjustCounterBatchCommand.TargetField.SUBSCRIBED_COUNT);

        // 2. ADIM: Takip Edilenlerin 'subscriberCount' (Takipçi) sayısını güncelle
        processChunk(targetUserId, action, AdjustCounterBatchCommand.TargetField.SUBSCRIBER_COUNT);
    }

    private void processChunk(Long targetUserId, AdjustCounterBatchCommand.Action action, AdjustCounterBatchCommand.TargetField targetField) {
        Long lastId = 0L;
        int pageSize = 2000;
        List<Long> chunk;

        do {
            chunk = (targetField == AdjustCounterBatchCommand.TargetField.SUBSCRIBED_COUNT)
                    ? subscribeRepo.findFollowerIdsKeyset(targetUserId, lastId, PageRequest.of(0, pageSize))
                    : subscribeRepo.findSubscribedIdsKeyset(targetUserId, lastId, PageRequest.of(0, pageSize));

            if (!chunk.isEmpty()) {
                lastId = chunk.get(chunk.size() - 1);
                AdjustCounterBatchCommand command = new AdjustCounterBatchCommand(targetUserId, chunk, action, targetField);
                kafkaTemplate.send(KafkaUserConfig.USER_COUNTER_ADJUSTMENT_COMMANDS_TOPIC, command);
            }
        } while (chunk.size() == pageSize);
    }
}