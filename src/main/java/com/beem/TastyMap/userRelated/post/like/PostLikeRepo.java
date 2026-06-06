package com.beem.TastyMap.userRelated.post.like;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostLikeRepo extends JpaRepository<PostLikeEntity,Long> ,PostLikeRepoCustom {
    @Query("SELECT l.id FROM PostLikeEntity l WHERE l.post.id = :postId AND l.user.id = :userId")
    Optional<Long> findIdByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);

}
