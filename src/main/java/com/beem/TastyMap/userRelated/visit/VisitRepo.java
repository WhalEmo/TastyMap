package com.beem.TastyMap.userRelated.visit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRepo extends JpaRepository<VisitEntity,Long> ,VisitRepoCustom{

}
