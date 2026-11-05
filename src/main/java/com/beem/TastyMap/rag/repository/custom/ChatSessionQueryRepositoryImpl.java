package com.beem.TastyMap.rag.repository.custom;

import com.beem.TastyMap.rag.data.response.AiSessionSummaryResponse;
import com.beem.TastyMap.rag.entity.QChatSessionEntity;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ChatSessionQueryRepositoryImpl implements ChatSessionQueryRepository {

    private final JPAQueryFactory queryFactory;

    public ChatSessionQueryRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Slice<AiSessionSummaryResponse> findSessionsByUserId(Long userId, Pageable pageable) {
        QChatSessionEntity session = QChatSessionEntity.chatSessionEntity;
        int pageSize = pageable.getPageSize();

        // Doğrudan DTO Projection ile sadece 4 kolonu SELECT ediyoruz
        List<AiSessionSummaryResponse> results = queryFactory
                .select(Projections.constructor(
                        AiSessionSummaryResponse.class,
                        session.id,
                        session.title,
                        session.createdAt,
                        session.updatedAt
                ))
                .from(session)
                .where(session.userId.eq(userId))
                .orderBy(session.updatedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageSize + 1)
                .fetch();

        boolean hasNext = results.size() > pageSize;
        if (hasNext) {
            results.remove(pageSize); // Ekstra çekilen elemanı çıkarıyoruz
        }

        return new SliceImpl<>(results, pageable, hasNext);
    }
}