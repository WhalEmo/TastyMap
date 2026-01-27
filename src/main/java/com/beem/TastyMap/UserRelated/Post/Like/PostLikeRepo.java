package com.beem.TastyMap.UserRelated.Post.Like;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepo extends JpaRepository<PostLikeEntity,Long> {
    Optional<PostLikeEntity> findByPost_IdAndUser_Id(Long postId, Long userId);
}
