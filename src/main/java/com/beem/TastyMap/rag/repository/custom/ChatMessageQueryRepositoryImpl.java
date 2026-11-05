package com.beem.TastyMap.rag.repository.custom;

import com.beem.TastyMap.rag.entity.ChatMessageEntity;
import com.beem.TastyMap.rag.entity.QChatMessageEntity;
import com.beem.TastyMap.rag.entity.QChatSessionEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ChatMessageQueryRepositoryImpl implements ChatMessageQueryRepository {

    private final JPAQueryFactory queryFactory;

    public ChatMessageQueryRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<ChatMessageEntity> findMessagesBySessionAndUser(String sessionId, Long userId) {
        QChatMessageEntity message = QChatMessageEntity.chatMessageEntity;
        QChatSessionEntity session = QChatSessionEntity.chatSessionEntity;

        return queryFactory
                .selectFrom(message)
                .innerJoin(message.session, session)
                .where(
                        session.id.eq(sessionId),
                        session.userId.eq(userId)
                )
                .orderBy(message.createdAt.asc())
                .fetch();
    }
}