package com.beem.TastyMap.post.repo;

import com.beem.TastyMap.post.dto.PostGridResponseDTO;
import com.beem.TastyMap.post.dto.PostResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostRepoCustom {
    Page<PostGridResponseDTO> findUserGridPosts(Long targetUserId, Pageable pageable);
}
