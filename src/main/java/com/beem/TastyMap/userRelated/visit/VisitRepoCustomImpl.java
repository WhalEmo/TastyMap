package com.beem.TastyMap.userRelated.visit;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class VisitRepoCustomImpl implements VisitRepoCustom{
    private final JPAQueryFactory queryFactory;

    public VisitRepoCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<VisitResponseDTO> findUserVisits(Long userId, Pageable pageable) {
        QVisitEntity visit = QVisitEntity.visitEntity;

        List<VisitResponseDTO> results = queryFactory
                .select(Projections.constructor(VisitResponseDTO.class,
                        visit.id,
                        visit.createdAt,
                        visit.placeEmbedded.placeId,
                        visit.placeEmbedded.placeName,
                        visit.placeEmbedded.categories,
                        visit.placeEmbedded.city,
                        visit.placeEmbedded.district,
                        visit.placeEmbedded.neighbourhood,
                        visit.placeEmbedded.latitude,
                        visit.placeEmbedded.longitude,
                        visit.placeEmbedded.averagePuan
                ))
                .from(visit)
                .where(visit.user.id.eq(userId).and(visit.isDelete.isFalse()))
                .orderBy(visit.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .select(visit.count())
                .from(visit)
                .where(visit.user.id.eq(userId))
                .fetchOne();

        return new PageImpl<>(results, pageable, total);
    }
}
