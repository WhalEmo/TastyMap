package com.beem.TastyMap.Security.Verification.ForgotPassword;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordRepo extends JpaRepository<PasswordEntity,Long> {
    Optional<PasswordEntity>findByToken(String token);
    @Modifying
    @Query("""
        DELETE FROM PasswordEntity p
        WHERE p.user.id = :userId
    """)
    void deleteAllByUserId(@Param("userId") Long userId);
}
