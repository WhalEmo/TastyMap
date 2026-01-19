package com.beem.TastyMap.Security.Verification.EmailVerify;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailRepo extends JpaRepository<EmailEntitiy,Long> {
    Optional<EmailEntitiy> findByToken(String token);
}
