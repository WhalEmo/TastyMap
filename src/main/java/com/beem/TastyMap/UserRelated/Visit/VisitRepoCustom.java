package com.beem.TastyMap.UserRelated.Visit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VisitRepoCustom {
    Page<VisitResponseDTO> findUserVisits(Long userId, Pageable pageable);
}
