package com.beem.TastyMap.rag.repository;

import com.beem.TastyMap.rag.entity.ChatMessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    List<ChatMessageEntity> findBySession_IdOrderByCreatedAtDesc(String sessionId, Pageable pageable);
}