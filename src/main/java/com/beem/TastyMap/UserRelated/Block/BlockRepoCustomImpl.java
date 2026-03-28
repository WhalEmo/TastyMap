package com.beem.TastyMap.UserRelated.Block;

import com.beem.TastyMap.RegisterLogin.QUserEntity;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class BlockRepoCustomImpl implements BlockRepoCustom{
    private final JPAQueryFactory queryFactory;

    public BlockRepoCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<BlockDTOResponse> findMyBlocks(Long myId, Pageable pageable) {
        QBlockEntity block = QBlockEntity.blockEntity;
        QUserEntity user = QUserEntity.userEntity;

        List<BlockDTOResponse> content = queryFactory
                .select(Projections.constructor(BlockDTOResponse.class,
                        user.id,
                        user.username,
                        user.profile,
                        block.createdAt))
                .from(block)
                .join(block.blocked, user)
                .where(block.blocker.id.eq(myId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(block.createdAt.desc())
                .fetch();

        long total = queryFactory
                .select(block.count())
                .from(block)
                .where(block.blocker.id.eq(myId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }
}
