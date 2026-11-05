package com.beem.TastyMap.rag.repository.custom;

import com.beem.TastyMap.rag.data.response.AiSessionSummaryResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface ChatSessionQueryRepository {
    Slice<AiSessionSummaryResponse> findSessionsByUserId(Long userId, Pageable pageable);
}