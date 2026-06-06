package com.beem.TastyMap.userRelated.post.comments.Like;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LikeRepo extends JpaRepository<LikeEntity,Long> {
    @Query("SELECT l.id FROM LikeEntity l WHERE l.comment.id = :commentId AND l.user.id = :userId")
    Optional<Long> findIdByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") Long userId);
    boolean existsByCommentIdAndUserId(Long commentId, Long userId);
}
