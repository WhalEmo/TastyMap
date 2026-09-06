package com.beem.TastyMap.userRelated.socialnotifications;

import com.beem.TastyMap.registerLogin.QUserEntity;
import com.beem.TastyMap.userRelated.socialnotifications.enums.NotificationActionStatus;
import com.beem.TastyMap.userRelated.socialnotifications.enums.SocialNotificationType;
import com.beem.TastyMap.userRelated.subscribe.RelationStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SocialNotificationRepoCustomImpl implements SocialNotificationRepoCustom {

    private final JPAQueryFactory queryFactory;

    public SocialNotificationRepoCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<SocialNotificationDTO> findByRecipientId(Long recipientId, Pageable pageable) {
        QSocialNotificationEntity notification = QSocialNotificationEntity.socialNotificationEntity;
        QUserEntity actor = new QUserEntity("actor");

        List<SocialNotificationDTO> content = queryFactory
                .select(
                        Projections.constructor(SocialNotificationDTO.class,
                                notification.id,
                                notification.type,
                                notification.actionStatus,
                                notification.isRead,
                                notification.createdAt,
                                Projections.constructor(SocialNotificationDTO.ActorDTO.class,
                                        actor.id,
                                        actor.username,
                                        actor.profile,
                                        Expressions.nullExpression(RelationStatus.class)
                                ),
                                Projections.constructor(SocialNotificationDTO.TargetDTO.class,
                                        notification.targetId,
                                        Expressions.nullExpression(String.class),
                                        notification.content
                                )
                        )
                )
                .from(notification)
                .innerJoin(notification.actor, actor)
                .where(notification.recipient.id.eq(recipientId))
                .orderBy(notification.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = queryFactory
                .select(notification.count())
                .from(notification)
                .where(notification.recipient.id.eq(recipientId))
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public void markAsRead(Long recipientId) {
        QSocialNotificationEntity notification = QSocialNotificationEntity.socialNotificationEntity;

        queryFactory.update(notification)
                .set(notification.isRead, true)
                .where(
                        notification.recipient.id.eq(recipientId),
                        notification.isRead.eq(false) // OPTİMİZASYON: Zaten okunanları tekrar güncellemeye çalışma
                )
                .execute();
    }

    @Override
    public void updateActionStatus(Long recipientId, Long actorId, SocialNotificationType type, NotificationActionStatus status) {
        QSocialNotificationEntity notification = QSocialNotificationEntity.socialNotificationEntity;
        queryFactory.update(notification)
                .set(notification.actionStatus, status)
                .where(
                        notification.recipient.id.eq(recipientId),
                        notification.actor.id.eq(actorId),
                        notification.type.eq(type)
                )
                .execute();
    }
}