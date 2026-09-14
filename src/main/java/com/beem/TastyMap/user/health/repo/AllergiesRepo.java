package com.beem.TastyMap.user.health.repo;

import com.beem.TastyMap.user.health.entity.AllergiesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllergiesRepo extends JpaRepository<AllergiesEntity,Long> {

}
