package com.beem.TastyMap.post.repo;

import com.beem.TastyMap.post.dto.PostGridResponseDTO;
import com.beem.TastyMap.post.entity.QPostEntity;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public class PostRepoCustomImpl implements PostRepoCustom {
    private final JPAQueryFactory queryFactory;

    public PostRepoCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<PostGridResponseDTO> findUserGridPosts(Long targetUserId, Pageable pageable) {
        QPostEntity post = QPostEntity.postEntity;

        // ElementCollection içindeki fotoğraflar için sanal yol
        StringPath photo = Expressions.stringPath("photo");

        List<PostGridResponseDTO> content = queryFactory
                .select(Projections.constructor(PostGridResponseDTO.class,
                        post.id,
                        photo.min(), // Fotoğraflardan ilkini/birini seçer (fotoğraf yoksa null döner)
                        post.createdAt,
                        post.isPinned
                ))
                .from(post)
                .leftJoin(post.photoUrls, photo) // Fotoğraf listesine LEFT JOIN atıyoruz
                .where(post.user.id.eq(targetUserId))
                .groupBy(post.id, post.isPinned, post.createdAt) // Gruplama ekliyoruz
                .orderBy(
                        post.isPinned.desc(),
                        post.createdAt.desc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
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