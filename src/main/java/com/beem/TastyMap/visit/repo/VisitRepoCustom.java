package com.beem.TastyMap.visit.repo;

import com.beem.TastyMap.visit.dto.VisitResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VisitRepoCustom {
    Page<VisitResponseDTO> findUserVisits(Long userId, Pageable pageable);
}
