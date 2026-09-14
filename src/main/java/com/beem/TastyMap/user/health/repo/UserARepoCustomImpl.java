package com.beem.TastyMap.user.health.repo;

import com.beem.TastyMap.user.health.dto.AllergyInfoDTO;
import com.beem.TastyMap.userRelated.health.entity.QAllergiesEntity;
import com.beem.TastyMap.userRelated.health.entity.QUserAllergiesEntity;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import java.util.List;

public class UserARepoCustomImpl implements UserARepoCustom{
    private final JPAQueryFactory queryFactory;

    public UserARepoCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<AllergyInfoDTO> findAllergyInfoByUserId(Long userId) {
        QUserAllergiesEntity userAllergy = QUserAllergiesEntity.userAllergiesEntity;
        QAllergiesEntity allergy = QAllergiesEntity.allergiesEntity;

        return queryFactory
                .select(Projections.constructor(AllergyInfoDTO.class,
                        allergy.id,
                        allergy.allergyName))
                .from(userAllergy)
                .join(userAllergy.allergies, allergy)
                .where(userAllergy.user.id.eq(userId))
                .fetch();
    }




}
