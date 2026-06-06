package com.beem.TastyMap.userRelated.post.like;

import com.beem.TastyMap.registerLogin.QUserEntity;
import com.beem.TastyMap.userRelated.block.QBlockEntity;
import com.beem.TastyMap.userRelated.subscribe.QSubscribeEntity;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class PostLikeRepoCustomImpl implements PostLikeRepoCustom {
    private final JPAQueryFactory queryFactory;

    public PostLikeRepoCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<PostLikeUserDTO> findPostLikesFullOrdered(Long postId, Long myId, Pageable pageable) {
        QPostLikeEntity pl= QPostLikeEntity.postLikeEntity;
        QUserEntity u = QUserEntity.userEntity;

        QSubscribeEntity s1 = new QSubscribeEntity("s1");
        QSubscribeEntity s2 = new QSubscribeEntity("s2");
        QBlockEntity b1 = new QBlockEntity("b1");
        QBlockEntity b2 = new QBlockEntity("b2");

        List<PostLikeUserDTO> content= queryFactory
                .select(Projections.constructor(PostLikeUserDTO.class,
                        u.id,
                        u.username,
                        u.profile,
                        s1.id.isNotNull(),
                        s2.id.isNotNull(),
                        s1.id.isNotNull().and(s2.id.isNotNull())
                        ))
                .from(pl)
                .join(pl.user, u)
                .leftJoin(s1).on(s1.subscribed.id.eq(u.id).and(s1.subscriber.id.eq(myId)))
                .leftJoin(s2).on(s2.subscribed.id.eq(myId).and(s2.subscriber.id.eq(u.id)))
                .leftJoin(b1).on(b1.blocker.id.eq(myId).and(b1.blocked.id.eq(u.id)))
                .leftJoin(b2).on(b2.blocker.id.eq(u.id).and(b2.blocked.id.eq(myId)))

                .where(
                        pl.post.id.eq(postId),
                        b1.id.isNull(),
                        b2.id.isNull()
                )
                .orderBy(
                        new CaseBuilder()
                                .when(u.id.eq(myId)).then(0)
                                .when(s1.id.isNotNull().and(s2.id.isNotNull())).then(1)
                                .when(s1.id.isNotNull()).then(2)
                                .otherwise(3).asc(),
                        u.username.asc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .select(pl.count())
                .from(pl)
                .join(pl.user, u)
                .leftJoin(b1).on(b1.blocker.id.eq(myId).and(b1.blocked.id.eq(u.id)))
                .leftJoin(b2).on(b2.blocker.id.eq(u.id).and(b2.blocked.id.eq(myId)))
                .where(
                        pl.post.id.eq(postId),
                        b1.id.isNull(),
                        b2.id.isNull()
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }
}
