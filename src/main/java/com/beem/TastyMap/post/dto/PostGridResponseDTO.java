package com.beem.TastyMap.post.dto;

public record PostGridResponseDTO(
        Long postId,
        String photoUrl,
        boolean isPinned
) {}
