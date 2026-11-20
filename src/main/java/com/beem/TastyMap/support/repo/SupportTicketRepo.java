package com.beem.TastyMap.support.repo;

import com.beem.TastyMap.support.entity.SupportTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;

public interface SupportTicketRepo extends JpaRepository<SupportTicketEntity, Long> {
    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime since);
}