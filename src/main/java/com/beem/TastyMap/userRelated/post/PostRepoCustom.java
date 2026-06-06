package com.beem.TastyMap.userRelated.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostRepoCustom {
    Page<PostResponseDTO> getUserPosts(Long userId, Long myId, Pageable pageable);
}
