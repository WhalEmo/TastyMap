package com.beem.TastyMap.userRelated.post.like;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostLikeRepoCustom {
    Page<PostLikeUserDTO> findPostLikesFullOrdered(Long postId, Long myId, Pageable pageable);
}
