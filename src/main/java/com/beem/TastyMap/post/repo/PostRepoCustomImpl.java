package com.beem.TastyMap.post.repo;


import com.beem.TastyMap.post.dto.PostGridResponseDTO;
import com.beem.TastyMap.post.dto.PostResponseDTO;
import com.beem.TastyMap.post.entity.QPostEntity;
import com.beem.TastyMap.post.like.QPostLikeEntity;
import com.beem.TastyMap.user.account.entity.QUserEntity;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public class PostRepoCustomImpl implements PostRepoCustom{
    private final JPAQueryFactory queryFactory;

    public PostRepoCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<PostGridResponseDTO> findUserGridPosts(Long targetUserId, Pageable pageable) {
        QPostEntity post = QPostEntity.postEntity;

        List<PostGridResponseDTO> content = queryFactory
                .select(Projections.constructor(PostGridResponseDTO.class,
                        post.id,
                        post.photoUrl,
                        post.isPinned
                ))
                .from(post)
                .where(post.user.id.eq(targetUserId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(
                        post.isPinned.desc(),
                        post.createdAt.desc()
                )
                .fetch();

        long total = Optional.ofNullable(
                queryFactory
                        .select(post.count())
                        .from(post)
                        .where(post.user.id.eq(targetUserId))
                        .fetchOne()
        ).orElse(0L);

        return new PageImpl<>(content, pageable, total);
    }
}
