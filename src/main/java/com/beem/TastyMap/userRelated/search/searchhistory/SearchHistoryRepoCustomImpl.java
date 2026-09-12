package com.beem.TastyMap.userRelated.search.searchhistory;

import com.beem.TastyMap.registerLogin.QUserEntity;
import com.beem.TastyMap.userRelated.search.UserSearchDTO;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
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
    public List<UserSearchDTO> findMyRecentSearches(Long myId, Pageable pageable) {
        QSearchHistoryEntity history = QSearchHistoryEntity.searchHistoryEntity;
        QUserEntity searchedUser = new QUserEntity("searchedUser");

        return queryFactory
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
    }
}
