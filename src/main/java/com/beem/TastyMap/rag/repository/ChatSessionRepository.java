package com.beem.TastyMap.rag.repository;

import com.beem.TastyMap.rag.entity.ChatSessionEntity;
import com.beem.TastyMap.rag.repository.custom.ChatSessionQueryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, String>, ChatSessionQueryRepository
{
    List<ChatSessionEntity> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<ChatSessionEntity> findByIdAndUserId(String id, Long userId);

    @Modifying
    @Query("UPDATE ChatSessionEntity s SET s.totalTokens = s.totalTokens + :tokens, " +
            "s.totalCostTry = s.totalCostTry + :cost, s.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE s.id = :sessionId")
    void incrementSessionTotals(@Param("sessionId") String sessionId,
                                @Param("tokens") int tokens,
                                @Param("cost") BigDecimal cost);
}