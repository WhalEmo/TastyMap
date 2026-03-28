package com.beem.TastyMap.UserRelated.Post.Like;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostLikeRepoCustom {
    Page<PostLikeUserDTO> findPostLikesFullOrdered(Long postId, Long myId, Pageable pageable);
}
