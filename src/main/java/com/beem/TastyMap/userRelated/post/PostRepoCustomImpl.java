package com.beem.TastyMap.userRelated.post;

import com.beem.TastyMap.registerLogin.QUserEntity;
import com.beem.TastyMap.userRelated.post.like.QPostLikeEntity;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class PostRepoCustomImpl implements PostRepoCustom{
    private final JPAQueryFactory queryFactory;

    public PostRepoCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<PostResponseDTO> getUserPosts(Long userId, Long myId, Pageable pageable) {
        QPostEntity post = QPostEntity.postEntity;
        QUserEntity user = QUserEntity.userEntity;
        QPostLikeEntity like = QPostLikeEntity.postLikeEntity;

        List<PostResponseDTO> content = queryFactory
                .select(Projections.constructor(PostResponseDTO.class,
                        post.commentEnabled,
                        post.id,
                        post.explanation,
                        post.puan,
                        post.photoUrl,
                        post.numberofLikes,
                        post.createdAt,
                        post.updateDate,
                        user.id,
                        user.username,
                        user.profile,
                        post.placeEmbedded.placeId,
                        post.placeEmbedded.placeName,
                        post.placeEmbedded.categories,
                        post.placeEmbedded.city,
                        post.placeEmbedded.district,
                        post.placeEmbedded.neighbourhood,
                        post.placeEmbedded.latitude,
                        post.placeEmbedded.longitude,
                        post.placeEmbedded.averagePuan,
                        JPAExpressions.selectOne()
                                .from(like)
                                .where(like.post.id.eq(post.id).and(like.user.id.eq(myId)))
                                .exists(),
                        post.commentCount,
                        post.isPinned
                ))
                .from(post)
                .join(post.user, user)
                .where(user.id.eq(userId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(post.createdAt.desc())
                .fetch();

        long total = queryFactory
                .select(post.count())
                .from(post)
                .where(post.user.id.eq(userId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }
}
