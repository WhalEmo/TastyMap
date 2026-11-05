package com.beem.TastyMap.rag.data.response;

import com.beem.TastyMap.rag.data.enums.MessageRole;
import java.time.LocalDateTime;
import java.util.List;

public record AiChatMessageResponse(
        Long id,
        MessageRole role,
        String content,
        LocalDateTime createdAt,
        List<HydratedPlaceCardDto> places
) {}