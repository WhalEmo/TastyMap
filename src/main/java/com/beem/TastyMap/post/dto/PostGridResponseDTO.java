package com.beem.TastyMap.post.dto;

import java.time.LocalDateTime;

public record PostGridResponseDTO(
        Long postId,
        String photoUrl,
        LocalDateTime createdAt,
        Boolean isPinned
) {}