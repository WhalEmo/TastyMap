package com.beem.TastyMap.userRelated.health.repos;

import com.beem.TastyMap.userRelated.health.entitys.UserAllergiesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserAllergiesRepo extends JpaRepository<UserAllergiesEntity,Long>,UserARepoCustom {

    @Query("SELECT ua FROM UserAllergiesEntity ua JOIN FETCH ua.allergies " +
            "WHERE ua.user.id = :userId")
    List<UserAllergiesEntity> findAllByUserIdWithAllergies(@Param("userId") Long userId);

    List<UserAllergiesEntity> findByUserId(Long userId);


}
