package com.beem.TastyMap.user.search.dto;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IdProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FieldProjection;

@ProjectionConstructor
public record UserSearchDTO(
        @IdProjection Long id,
        @FieldProjection String username,
        @FieldProjection String name,
        @FieldProjection String profile
) {
}