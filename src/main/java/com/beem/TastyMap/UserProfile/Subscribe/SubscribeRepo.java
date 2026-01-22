package com.beem.TastyMap.UserProfile.Subscribe;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscribeRepo extends JpaRepository<SubscribeEntity,Long> {
    boolean existsBySubscriberIdAndSubscribedId(Long sbscrbrId,Long sbscrbdId);
    Optional<SubscribeEntity>findBySubscriberIdAndSubscribedId(Long sbscrbrId,Long sbscrbdId);
    long countBySubscriberId(Long subscriberId);
    long countBySubscribedId(Long subscribedId);
}
