package com.beem.TastyMap.rag.repository;

import com.beem.TastyMap.rag.entity.ChatSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, String> {
    List<ChatSessionEntity> findAllByUserIdOrderByUpdatedAtDesc(Long userId);
}