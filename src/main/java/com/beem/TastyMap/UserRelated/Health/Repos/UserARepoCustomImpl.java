package com.beem.TastyMap.UserRelated.Health.Repos;

import com.beem.TastyMap.UserRelated.Health.DTOs.AllergyInfoDTO;
import com.beem.TastyMap.UserRelated.Health.DTOs.HealthResponseDTO;
import com.beem.TastyMap.UserRelated.Health.Entitys.QAllergiesEntity;
import com.beem.TastyMap.UserRelated.Health.Entitys.QUserAllergiesEntity;
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
