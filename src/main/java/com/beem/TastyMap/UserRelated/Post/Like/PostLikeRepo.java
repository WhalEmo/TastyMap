package com.beem.TastyMap.UserRelated.Post.Like;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostLikeRepo extends JpaRepository<PostLikeEntity,Long> {
    Optional<PostLikeEntity> findByPost_IdAndUser_Id(Long postId, Long userId);

    @Query("""
     select new com.beem.TastyMap.UserRelated.Post.PostLikeUserDTO(
       u.id,
       u.username,
       u.profile,
       
       CASE WHEN s1.id IS NOT NULL THEN true ELSE false END,
       CASE WHEN s2.id IS NOT NULL THEN true ELSE false END,
       CASE WHEN s1.id IS NOT NULL AND s2.id IS NOT NULL THEN true ELSE false END
       )
     FROM PostLikeEntity pl
     JOIN UserEntity u ON u.id=pl.user.id
     
            LEFT JOIN SubscribeEntity s1
                ON s1.subscribed.id = u.id
               AND s1.subscriber.id = :myId
            
          
            LEFT JOIN SubscribeEntity s2
                ON s2.subscribed.id = :myId
               AND s2.subscriber.id = u.id
            
          
            LEFT JOIN BlockEntity b1
                ON b1.blocker.id = :myId
               AND b1.blocked.id = u.id
            
          
            LEFT JOIN BlockEntity b2
                ON b2.blocker.id = u.id
               AND b2.blocked.id = :myId
               
               WHERE pl.postId = :postId
                        AND b1.id IS NULL
                        AND b2.id IS NULL
            
                      ORDER BY
                          CASE
                              WHEN u.id = :myId THEN 0
                              WHEN s1.id IS NOT NULL AND s2.id IS NOT NULL THEN 1
                              WHEN s1.id IS NOT NULL THEN 2
                              ELSE 3
                          END,
                          u.username ASC
                      """)
    Page<PostLikeUserDTO> findPostLikesFullOrdered(
                              @Param("postId") Long postId,
                              @Param("myId") Long myId,
                              Pageable pageable
                      );

}
