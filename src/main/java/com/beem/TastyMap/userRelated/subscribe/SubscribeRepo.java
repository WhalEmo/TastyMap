package com.beem.TastyMap.userRelated.subscribe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SubscribeRepo extends JpaRepository<SubscribeEntity,Long>,SubscribeRepoCustom {
    boolean existsBySubscriber_IdAndSubscribed_Id(Long subscriberId, Long subscribedId);

    @Query("SELECT s.id FROM SubscribeEntity s WHERE s.subscriber.id = :myId AND s.subscribed.id = :targetId")
    Optional<Long> findIdBySubscriberAndSubscribed(@Param("myId") Long myId, @Param("targetId") Long targetId);

    @Modifying
    @Query("DELETE FROM SubscribeEntity s WHERE s.subscriber.id = :suberId AND s.subscribed.id = :subedId")
    int deleteAndCount(@Param("suberId") Long suberId, @Param("subedId") Long subedId);

    Optional<SubscribeEntity> findBySubscriber_IdAndSubscribed_Id(Long subscriberId, Long subscribedId);

    boolean existsBySubscriber_IdAndSubscribed_IdAndStatus(Long subscriberId, Long subscribedId, SubscribeStatus status);

    @Query("SELECT s.status FROM SubscribeEntity s WHERE s.subscriber.id = :subscriberId AND s.subscribed.id = :subscribedId")
    Optional<SubscribeStatus> findStatusBySubscriberIdAndSubscribedId(
            @Param("subscriberId") Long subscriberId,
            @Param("subscribedId") Long subscribedId
    );

    @Query("SELECT s FROM SubscribeEntity s WHERE " +
            "(s.subscriber.id = :userA AND s.subscribed.id = :userB) OR " +
            "(s.subscriber.id = :userB AND s.subscribed.id = :userA)")
    List<SubscribeEntity> findRelationsBetween(@Param("userA") Long userA, @Param("userB") Long userB);

    @Query("SELECT s FROM SubscribeEntity s WHERE " +
            "(s.subscriber.id = :myId AND s.subscribed.id IN :actorIds) OR " +
            "(s.subscriber.id IN :actorIds AND s.subscribed.id = :myId)")
    List<SubscribeEntity> findRelationsBetweenMyIdAndActors(@Param("myId") Long myId, @Param("actorIds") Set<Long> actorIds);

}
