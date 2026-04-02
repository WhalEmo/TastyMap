package com.beem.TastyMap.UserRelated.Health.Repos;

import com.beem.TastyMap.UserRelated.Health.Entitys.UserHealthEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserHealthRepo extends JpaRepository<UserHealthEntity,Long> {
    Optional<UserHealthEntity>findByUserId(Long userId);
}
