package com.beem.TastyMap.Security.Verification.EmailVerify;

import com.beem.TastyMap.RegisterLogin.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface EmailRepo extends JpaRepository<EmailEntitiy,Long> {
    Optional<EmailEntitiy> findByToken(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmailEntitiy e WHERE e.user = :user")
    void deleteByUser(UserEntity user);
}
