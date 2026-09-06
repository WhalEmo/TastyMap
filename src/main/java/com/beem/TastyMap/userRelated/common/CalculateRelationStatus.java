package com.beem.TastyMap.userRelated.common;

import com.beem.TastyMap.userRelated.subscribe.RelationStatus;
import com.beem.TastyMap.userRelated.subscribe.SubscribeRepo;
import com.beem.TastyMap.userRelated.subscribe.SubscribeStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CalculateRelationStatus {

    private final SubscribeRepo subscribeRepo;

    public CalculateRelationStatus(SubscribeRepo subscribeRepo) {
        this.subscribeRepo = subscribeRepo;
    }

    public RelationStatus calculate(Long myId, Long targetUserId) {
        Optional<SubscribeStatus> myRequestStatus = subscribeRepo.findStatusBySubscriberIdAndSubscribedId(myId, targetUserId);

        if (myRequestStatus.isPresent()) {
            return myRequestStatus.get() == SubscribeStatus.ACCEPTED
                    ? RelationStatus.FOLLOWING
                    : RelationStatus.PENDING;
        }

        boolean isFollower = subscribeRepo.existsBySubscriber_IdAndSubscribed_IdAndStatus(
                targetUserId, myId, SubscribeStatus.ACCEPTED
        );

        return isFollower ? RelationStatus.FOLLOW_BACK : RelationStatus.NOT_FOLLOWING;
    }
}