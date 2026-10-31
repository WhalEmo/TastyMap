package com.beem.TastyMap.user.subscribe.repo;

import com.beem.TastyMap.user.account.entity.QUserEntity;
import com.beem.TastyMap.user.block.entity.QBlockEntity;
import com.beem.TastyMap.user.subscribe.entity.QSubscribeEntity;
import com.beem.TastyMap.user.subscribe.model.RelationStatus;
import com.beem.TastyMap.user.subscribe.model.SubscribeStatus;
import com.beem.TastyMap.user.subscribe.dto.SubscribeDTO;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
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

    private BooleanExpression isNotBlocked(Long myId, QUserEntity targetUser) {
        QBlockEntity block = QBlockEntity.blockEntity;
        return JPAExpressions.selectOne()
                .from(block)
                .where(
                        (block.blocker.id.eq(myId).and(block.blocked.id.eq(targetUser.id)))
                                .or(block.blocker.id.eq(targetUser.id).and(block.blocked.id.eq(myId)))
                ).notExists();
    }

    private Expression<String> buildRelationStatusExpression(
            Long myId,
            QUserEntity targetUser,
            QSubscribeEntity myRelation,
            QSubscribeEntity targetRelation
    ) {
        return new CaseBuilder()
                .when(targetUser.id.eq(myId)).then(RelationStatus.SELF.name())
                .when(myRelation.status.eq(SubscribeStatus.ACCEPTED)).then(RelationStatus.FOLLOWING.name())
                .when(myRelation.status.eq(SubscribeStatus.PENDING)).then(RelationStatus.PENDING.name())
                .when(targetRelation.status.eq(SubscribeStatus.ACCEPTED)
                        .and(myRelation.id.isNull().or(myRelation.status.ne(SubscribeStatus.ACCEPTED))))
                .then(RelationStatus.FOLLOW_BACK.name())
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
                        .and(subscribe.status.eq(SubscribeStatus.ACCEPTED))
                        .and(isNotBlocked(myId, user))) // BLOK FILTRESI EKLENDI
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(subscribe.count())
                .from(subscribe)
                .join(subscribe.subscribed, user) // User ile join yapmak zorundayiz ki ID'sine eriselim
                .where(subscribe.subscriber.id.eq(userId)
                        .and(subscribe.status.eq(SubscribeStatus.ACCEPTED))
                        .and(isNotBlocked(myId, user))) // COUNT SORGUSUNA DA EKLENDI
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
                        .and(subscribe.status.eq(SubscribeStatus.ACCEPTED))
                        .and(isNotBlocked(myId, user))) // BLOK FILTRESI EKLENDI
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(subscribe.count())
                .from(subscribe)
                .join(subscribe.subscriber, user)
                .where(subscribe.subscribed.id.eq(userId)
                        .and(subscribe.status.eq(SubscribeStatus.ACCEPTED))
                        .and(isNotBlocked(myId, user))) // COUNT SORGUSUNA DA EKLENDI
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
                        .and(subscribe.status.eq(SubscribeStatus.PENDING))
                        .and(isNotBlocked(myId, user))) // BLOK FILTRESI EKLENDI
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(subscribe.count())
                .from(subscribe)
                .join(subscribe.subscriber, user)
                .where(subscribe.subscribed.id.eq(myId)
                        .and(subscribe.status.eq(SubscribeStatus.PENDING))
                        .and(isNotBlocked(myId, user))) // COUNT SORGUSUNA DA EKLENDI
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}