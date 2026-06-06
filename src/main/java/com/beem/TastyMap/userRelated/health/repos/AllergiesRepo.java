package com.beem.TastyMap.userRelated.health.repos;

import com.beem.TastyMap.userRelated.health.entitys.AllergiesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllergiesRepo extends JpaRepository<AllergiesEntity,Long> {

}
