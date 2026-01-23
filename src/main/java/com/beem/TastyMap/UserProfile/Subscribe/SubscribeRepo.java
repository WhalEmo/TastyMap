package com.beem.TastyMap.UserProfile.Subscribe;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubscribeRepo extends JpaRepository<SubscribeEntity,Long> {
    boolean existsBySubscriberIdAndSubscribedId(Long sbscrbrId,Long sbscrbdId);
    Optional<SubscribeEntity>findBySubscriberIdAndSubscribedId(Long sbscrbrId,Long sbscrbdId);
    long countBySubscriberId(Long subscriberId);
    long countBySubscribedId(Long subscribedId);

    @Query("""
    SELECT new com.beem.TastyMap.UserProfile.Subscribe.SubscribeDTO(
        u.id,
        u.profile,
        u.username
    )
        FROM SubscribeEntity s
        JOIN UserEntity u ON u.id = s.subscribedId
        WHERE s.subscriberId = :userId
        """)
    Page<SubscribeDTO> findUserSubscribes(
            @Param("userId") Long userId,
            Pageable pageable
    );


    @Query("""
    SELECT new com.beem.TastyMap.UserProfile.Subscribe.SubscribeDTO(
        u.id,
        u.profile,
        u.username
    )
    FROM SubscribeEntity s
    JOIN UserEntity u ON u.id = s.subscriberId
    WHERE s.subscribedId = :userId
""")
    Page<SubscribeDTO>findUserSubscribers(
            @Param("userId") Long userId,
            Pageable pageable
    );


}
