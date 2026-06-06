package com.beem.TastyMap.userRelated.health.repos;

import com.beem.TastyMap.userRelated.health.entitys.UserHealthEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserHealthRepo extends JpaRepository<UserHealthEntity,Long> {
    Optional<UserHealthEntity>findByUserId(Long userId);
}
