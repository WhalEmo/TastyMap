package com.beem.TastyMap.UserRelated.Subscribe;

import com.beem.TastyMap.RegisterLogin.QUserEntity;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class SubscribeRepoCustomImpl implements SubscribeRepoCustom{
    private final JPAQueryFactory queryFactory;

    public SubscribeRepoCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<SubscribeDTO> findUserSubscribes(Long userId, Pageable pageable) {
        QSubscribeEntity subscribe = QSubscribeEntity.subscribeEntity;
        QUserEntity user = QUserEntity.userEntity;

        List<SubscribeDTO> content = queryFactory
                .select(Projections.constructor(SubscribeDTO.class,
                        user.id,
                        user.profile,
                        user.username
                ))
                .from(subscribe)
                .join(subscribe.subscribed, user)
                .where(subscribe.subscriber.id.eq(userId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(subscribe.count())
                .from(subscribe)
                .where(subscribe.subscriber.id.eq(userId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<SubscribeDTO> findUserSubscribers(Long userId, Pageable pageable) {
        QSubscribeEntity subscribe = QSubscribeEntity.subscribeEntity;
        QUserEntity user = QUserEntity.userEntity;
        List<SubscribeDTO> content = queryFactory
                .select(Projections.constructor(SubscribeDTO.class,
                        user.id,
                        user.profile,
                        user.username
                ))
                .from(subscribe)
                .join(subscribe.subscriber, user)
                .where(subscribe.subscribed.id.eq(userId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        Long total = queryFactory
                .select(subscribe.count())
                .from(subscribe)
                .where(subscribe.subscribed.id.eq(userId))
                .fetchOne();
        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }


}
