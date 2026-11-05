package com.beem.TastyMap.rag.repository;

import com.beem.TastyMap.rag.entity.AiTokenUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface AiTokenUsageRepository extends JpaRepository<AiTokenUsageEntity, Long> {

    // 1. Bir oturumun toplam maliyeti
    @Query("SELECT COALESCE(SUM(u.costTry), 0) FROM AiTokenUsageEntity u WHERE u.session.id = :sessionId")
    BigDecimal sumCostTryBySessionId(@Param("sessionId") String sessionId);

    // 2. Kullanıcının belirli iki tarih arasındaki toplam harcaması
    @Query("SELECT COALESCE(SUM(u.totalTokens), 0) FROM AiTokenUsageEntity u " +
            "WHERE u.userId = :userId AND u.createdAt BETWEEN :start AND :end")
    Long sumTokensByUserIdBetween(@Param("userId") Long userId,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);

    // 3. Model bazında toplam harcama raporu (Admin paneli için)
    @Query("SELECT u.modelName, SUM(u.totalTokens), SUM(u.costTry) FROM AiTokenUsageEntity u GROUP BY u.modelName")
    List<Object[]> getUsageReportByModel();
}