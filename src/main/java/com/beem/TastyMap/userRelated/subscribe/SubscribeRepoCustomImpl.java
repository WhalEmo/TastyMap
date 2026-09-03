package com.beem.TastyMap.userRelated.subscribe;

import com.beem.TastyMap.registerLogin.QUserEntity;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class SubscribeRepoCustomImpl implements SubscribeRepoCustom {
    private final JPAQueryFactory queryFactory;

    public SubscribeRepoCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<SubscribeDTO> findUserSubscribes(Long userId, Long myId, Pageable pageable) {
        QSubscribeEntity subscribe = QSubscribeEntity.subscribeEntity;
        QSubscribeEntity myRelation = new QSubscribeEntity("myRelation");
        QUserEntity user = QUserEntity.userEntity;

        List<SubscribeDTO> content = queryFactory
                .select(Projections.constructor(SubscribeDTO.class,
                        user.id,
                        user.profile,
                        user.username,
                        myRelation.status
                ))
                .from(subscribe)
                .join(subscribe.subscribed, user)
                .leftJoin(myRelation).on(
                        myRelation.subscriber.id.eq(myId)
                                .and(myRelation.subscribed.id.eq(user.id))
                )
                .where(subscribe.subscriber.id.eq(userId)
                        .and(subscribe.status.eq(SubscribeStatus.ACCEPTED)))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(subscribe.count())
                .from(subscribe)
                .where(subscribe.subscriber.id.eq(userId)
                        .and(subscribe.status.eq(SubscribeStatus.ACCEPTED)))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<SubscribeDTO> findUserSubscribers(Long userId, Long myId, Pageable pageable) {
        QSubscribeEntity subscribe = QSubscribeEntity.subscribeEntity;
        QSubscribeEntity myRelation = new QSubscribeEntity("myRelation");
        QUserEntity user = QUserEntity.userEntity;

        List<SubscribeDTO> content = queryFactory
                .select(Projections.constructor(SubscribeDTO.class,
                        user.id,
                        user.profile,
                        user.username,
                        myRelation.status
                ))
                .from(subscribe)
                .join(subscribe.subscriber, user)
                .leftJoin(myRelation).on(
                        myRelation.subscriber.id.eq(myId)
                                .and(myRelation.subscribed.id.eq(user.id))
                )
                .where(subscribe.subscribed.id.eq(userId)
                        .and(subscribe.status.eq(SubscribeStatus.ACCEPTED)))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(subscribe.count())
                .from(subscribe)
                .where(subscribe.subscribed.id.eq(userId)
                        .and(subscribe.status.eq(SubscribeStatus.ACCEPTED)))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<SubscribeDTO> findPendingRequests(Long userId, Pageable pageable) {
        QSubscribeEntity subscribe = QSubscribeEntity.subscribeEntity;
        QUserEntity user = QUserEntity.userEntity;

        List<SubscribeDTO> content = queryFactory
                .select(Projections.constructor(SubscribeDTO.class,
                        user.id,
                        user.profile,
                        user.username,
                        subscribe.status
                ))
                .from(subscribe)
                .join(subscribe.subscriber, user)
                .where(subscribe.subscribed.id.eq(userId)
                        .and(subscribe.status.eq(SubscribeStatus.PENDING)))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(subscribe.count())
                .from(subscribe)
                .where(subscribe.subscribed.id.eq(userId)
                        .and(subscribe.status.eq(SubscribeStatus.PENDING)))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}