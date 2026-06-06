package com.beem.TastyMap.userRelated.block;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface BlockRepoCustom {
    Page<BlockDTOResponse>findMyBlocks(@Param("myId") Long myId, Pageable pageable);
}
