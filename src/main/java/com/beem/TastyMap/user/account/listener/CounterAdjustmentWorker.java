package com.beem.TastyMap.user.account.listener;

import com.beem.TastyMap.user.account.event.AdjustCounterBatchCommand;
import com.beem.TastyMap.user.account.repo.UserRepo;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CounterAdjustmentWorker {

    private final UserRepo userRepo;

    public CounterAdjustmentWorker(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltTopicSuffix = "-dlq",
            autoCreateTopics = "true"
    )
    @KafkaListener(
            topics = "#{T(com.beem.TastyMap.user.account.config.KafkaUserConfig).USER_COUNTER_ADJUSTMENT_COMMANDS_TOPIC}",
            groupId = "counter-worker-group",
            concurrency = "5"
    )
    @Transactional
    public void processCounterBatch(AdjustCounterBatchCommand command) {
        if (command.getUserIdsToUpdate() == null || command.getUserIdsToUpdate().isEmpty()) {
            return;
        }

        boolean isDecrement = command.getAction() == AdjustCounterBatchCommand.Action.DECREMENT;

        if (command.getTargetField() == AdjustCounterBatchCommand.TargetField.SUBSCRIBED_COUNT) {
            if (isDecrement) {
                userRepo.decrementSubscribedCountsInBatch(command.getUserIdsToUpdate());
            } else {
                userRepo.incrementSubscribedCountsInBatch(command.getUserIdsToUpdate());
            }
        } else if (command.getTargetField() == AdjustCounterBatchCommand.TargetField.SUBSCRIBER_COUNT) {
            if (isDecrement) {
                userRepo.decrementSubscriberCountsInBatch(command.getUserIdsToUpdate());
            } else {
                userRepo.incrementSubscriberCountsInBatch(command.getUserIdsToUpdate());
            }
        }
    }
}