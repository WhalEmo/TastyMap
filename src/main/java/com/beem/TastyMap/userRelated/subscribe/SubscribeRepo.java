package com.beem.TastyMap.userRelated.subscribe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubscribeRepo extends JpaRepository<SubscribeEntity,Long>,SubscribeRepoCustom {
    boolean existsBySubscriber_IdAndSubscribed_Id(Long subscriberId, Long subscribedId);

    @Query("SELECT s.id FROM SubscribeEntity s WHERE s.subscriber.id = :myId AND s.subscribed.id = :targetId")
    Optional<Long> findIdBySubscriberAndSubscribed(@Param("myId") Long myId, @Param("targetId") Long targetId);

    @Modifying
    @Query("DELETE FROM SubscribeEntity s WHERE s.subscriber.id = :suberId AND s.subscribed.id = :subedId")
    int deleteAndCount(@Param("suberId") Long suberId, @Param("subedId") Long subedId);
}
