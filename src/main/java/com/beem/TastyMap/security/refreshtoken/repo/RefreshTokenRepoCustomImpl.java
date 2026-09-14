package com.beem.TastyMap.security.refreshtoken.repo;
import com.beem.TastyMap.security.device.QUserDeviceEntity;
import com.beem.TastyMap.security.refreshtoken.QRefreshTokenEntity;
import com.beem.TastyMap.user.profile.dto.ActiveDeviceDTO;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import java.util.List;

public class RefreshTokenRepoCustomImpl implements RefreshTokenRepoCustom{
    private final JPAQueryFactory queryFactory;

    public RefreshTokenRepoCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<ActiveDeviceDTO> findActiveDevices(Long userId) {
        QRefreshTokenEntity refresh = QRefreshTokenEntity.refreshTokenEntity;
        QUserDeviceEntity device = QUserDeviceEntity.userDeviceEntity;

        return queryFactory
                .select(Projections.constructor(
                        ActiveDeviceDTO.class,
                        device.deviceId,
                        device.userAgent,
                        device.lastCity,
                        device.lastLoginAt
                ))
                .from(refresh)
                .join(device)
                .on(
                        device.deviceId.eq(refresh.deviceId)
                                .and(device.user.id.eq(refresh.user.id))
                )
                .where(
                        refresh.user.id.eq(userId),
                        refresh.revoked.eq(false)
                )
                .fetch();
    }
}
