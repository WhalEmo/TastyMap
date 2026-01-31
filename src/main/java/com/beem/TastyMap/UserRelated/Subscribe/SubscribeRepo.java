package com.beem.TastyMap.UserRelated.Subscribe;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubscribeRepo extends JpaRepository<SubscribeEntity,Long> {
    boolean existsBySubscriber_IdAndSubscribed_Id(Long subscriberId, Long subscribedId);

    Optional<SubscribeEntity> findBySubscriber_IdAndSubscribed_Id(
            Long subscriberId,
            Long subscribedId
    );

    long countBySubscriber_Id(Long subscriberId);
    long countBySubscribed_Id(Long subscribedId);


    @Query("""
    SELECT new com.beem.TastyMap.UserRelated.Subscribe.SubscribeDTO(
        u.id,
        u.profile,
        u.username
    )
    FROM SubscribeEntity s
    JOIN s.subscribed u
    WHERE s.subscriber.id = :userId
""")
    Page<SubscribeDTO> findUserSubscribes(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
    SELECT new com.beem.TastyMap.UserRelated.Subscribe.SubscribeDTO(
        u.id,
        u.profile,
        u.username
    )
    FROM SubscribeEntity s
    JOIN s.subscriber u
    WHERE s.subscribed.id = :userId
""")
    Page<SubscribeDTO> findUserSubscribers(
            @Param("userId") Long userId,
            Pageable pageable
    );


    @Modifying
    @Query("""
DELETE FROM SubscribeEntity s
WHERE 
(s.subscriber.id = :myId AND s.subscribed.id = :userId)
OR
(s.subscriber.id = :userId AND s.subscribed.id = :myId)
""")
    void deleteMutualSubscribe(
            @Param("myId") Long myId,
            @Param("userId") Long userId
    );


}
