package com.beem.TastyMap.userRelated.subscribe;

import com.beem.TastyMap.registerLogin.QUserEntity;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
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

    // Enum yerine String dondurerek SqmParameter eşleme hatasını engelliyoruz
    private Expression<String> buildRelationStatusExpression(
            Long myId,
            QUserEntity targetUser,
            QSubscribeEntity myRelation,
            QSubscribeEntity targetRelation
    ) {
        return new CaseBuilder()
                // 1. Durum: Listelenen kullanıcı BENİM -> SELF
                .when(targetUser.id.eq(myId))
                .then(RelationStatus.SELF.name())

                // 2. Durum: Ben ona istek atmışım ve kabul edilmiş -> FOLLOWING
                .when(myRelation.status.eq(SubscribeStatus.ACCEPTED))
                .then(RelationStatus.FOLLOWING.name())

                // 3. Durum: Ben ona istek atmışım ama onay bekliyor -> PENDING
                .when(myRelation.status.eq(SubscribeStatus.PENDING))
                .then(RelationStatus.PENDING.name())

                // 4. Durum: Ben onu takip etmiyorum ama O BENİ takip ediyor -> FOLLOW_BACK
                .when(targetRelation.status.eq(SubscribeStatus.ACCEPTED)
                        .and(myRelation.id.isNull().or(myRelation.status.ne(SubscribeStatus.ACCEPTED))))
                .then(RelationStatus.FOLLOW_BACK.name())

                // 5. Durum: Hiçbir ilişki yok veya durumlar uymuyor -> NOT_FOLLOWING
                .otherwise(RelationStatus.NOT_FOLLOWING.name());
    }

    @Override
    public Page<SubscribeDTO> findUserSubscribes(Long userId, Long myId, Pageable pageable) {
        QSubscribeEntity subscribe = QSubscribeEntity.subscribeEntity;
        QSubscribeEntity myRelation = new QSubscribeEntity("myRelation");
        QSubscribeEntity targetRelation = new QSubscribeEntity("targetRelation");
        QUserEntity user = QUserEntity.userEntity;

        List<SubscribeDTO> content = queryFactory
                .select(Projections.constructor(SubscribeDTO.class,
                        user.id,
                        user.profile,
                        user.username,
                        buildRelationStatusExpression(myId, user, myRelation, targetRelation)
                ))
                .from(subscribe)
                .join(subscribe.subscribed, user)
                .leftJoin(myRelation).on(
                        myRelation.subscriber.id.eq(myId)
                                .and(myRelation.subscribed.id.eq(user.id))
                )
                .leftJoin(targetRelation).on(
                        targetRelation.subscriber.id.eq(user.id)
                                .and(targetRelation.subscribed.id.eq(myId))
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
        QSubscribeEntity targetRelation = new QSubscribeEntity("targetRelation");
        QUserEntity user = QUserEntity.userEntity;

        List<SubscribeDTO> content = queryFactory
                .select(Projections.constructor(SubscribeDTO.class,
                        user.id,
                        user.profile,
                        user.username,
                        buildRelationStatusExpression(myId, user, myRelation, targetRelation)
                ))
                .from(subscribe)
                .join(subscribe.subscriber, user)
                .leftJoin(myRelation).on(
                        myRelation.subscriber.id.eq(myId)
                                .and(myRelation.subscribed.id.eq(user.id))
                )
                .leftJoin(targetRelation).on(
                        targetRelation.subscriber.id.eq(user.id)
                                .and(targetRelation.subscribed.id.eq(myId))
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
    public Page<SubscribeDTO> findPendingRequests(Long myId, Pageable pageable) {
        QSubscribeEntity subscribe = QSubscribeEntity.subscribeEntity;
        QSubscribeEntity myRelation = new QSubscribeEntity("myRelation");
        QSubscribeEntity targetRelation = new QSubscribeEntity("targetRelation");
        QUserEntity user = QUserEntity.userEntity;

        List<SubscribeDTO> content = queryFactory
                .select(Projections.constructor(SubscribeDTO.class,
                        user.id,
                        user.profile,
                        user.username,
                        buildRelationStatusExpression(myId, user, myRelation, targetRelation)
                ))
                .from(subscribe)
                .join(subscribe.subscriber, user)
                .leftJoin(myRelation).on(
                        myRelation.subscriber.id.eq(myId)
                                .and(myRelation.subscribed.id.eq(user.id))
                )
                .leftJoin(targetRelation).on(
                        targetRelation.subscriber.id.eq(user.id)
                                .and(targetRelation.subscribed.id.eq(myId))
                )
                .where(subscribe.subscribed.id.eq(myId)
                        .and(subscribe.status.eq(SubscribeStatus.PENDING)))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(subscribe.count())
                .from(subscribe)
                .where(subscribe.subscribed.id.eq(myId)
                        .and(subscribe.status.eq(SubscribeStatus.PENDING)))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}