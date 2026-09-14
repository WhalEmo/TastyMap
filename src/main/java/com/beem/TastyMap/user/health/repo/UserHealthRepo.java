package com.beem.TastyMap.user.health.repo;

import com.beem.TastyMap.user.health.entity.UserHealthEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserHealthRepo extends JpaRepository<UserHealthEntity,Long> {
    Optional<UserHealthEntity>findByUserId(Long userId);
}
