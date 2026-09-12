package com.beem.TastyMap.userRelated.search;

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