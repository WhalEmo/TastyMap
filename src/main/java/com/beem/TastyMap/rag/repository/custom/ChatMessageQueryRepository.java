package com.beem.TastyMap.rag.repository.custom;

import com.beem.TastyMap.rag.entity.ChatMessageEntity;

import java.util.List;

public interface ChatMessageQueryRepository {
    List<ChatMessageEntity> findMessagesBySessionAndUser(String sessionId, Long userId);
}