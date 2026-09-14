package com.beem.TastyMap.user.search.repo;

import com.beem.TastyMap.user.QUserEntity;
import com.beem.TastyMap.user.search.dto.UserSearchDTO;
import com.beem.TastyMap.userRelated.search.searchhistory.QSearchHistoryEntity;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SearchHistoryRepoCustomImpl implements SearchHistoryRepoCustom {

    private final JPAQueryFactory queryFactory;

    public SearchHistoryRepoCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<UserSearchDTO> findMyRecentSearches(Long myId, Pageable pageable) {
        QSearchHistoryEntity history = QSearchHistoryEntity.searchHistoryEntity;
        QUserEntity searchedUser = new QUserEntity("searchedUser");

        List<UserSearchDTO> content = queryFactory
                .select(
                        Projections.constructor(
                                UserSearchDTO.class,
                                searchedUser.id,
                                searchedUser.username,
                                searchedUser.name,
                                searchedUser.profile
                        )
                )
                .from(history)
                .innerJoin(history.searchedUser, searchedUser)
                .where(history.searcher.id.eq(myId))
                .orderBy(history.searchedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        Long total = queryFactory
                .select(history.count())
                .from(history)
                .where(history.searcher.id.eq(myId))
                .fetchOne();
        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}