package com.beem.TastyMap.user.search.dto;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IdProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FieldProjection;

import java.time.LocalDateTime;

public record UserSearchDTO(
        Long id,
        String username,
        String name,
        String profile,
        LocalDateTime searchedAt
){
    @ProjectionConstructor
    public UserSearchDTO(
            @IdProjection Long id,
            @FieldProjection String username,
            @FieldProjection String name,
            @FieldProjection String profile
    ) {
        this(id, username, name, profile, null);
    }
}