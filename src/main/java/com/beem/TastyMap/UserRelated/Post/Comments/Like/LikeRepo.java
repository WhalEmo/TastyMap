package com.beem.TastyMap.UserRelated.Post.Comments.Like;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepo extends JpaRepository<LikeEntity,Long> {
    Optional<LikeEntity> findByComment_IdAndUser_Id(Long commentId, Long userId);
}
