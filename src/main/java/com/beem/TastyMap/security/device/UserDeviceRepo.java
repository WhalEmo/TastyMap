package com.beem.TastyMap.security.device;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserDeviceRepo extends JpaRepository<UserDeviceEntity,Long> {
    Optional<UserDeviceEntity>findByUser_IdAndDeviceId(Long userId, String deviceId);

    @Query("SELECT d.fcmToken FROM UserDeviceEntity d WHERE d.user.id = :userId AND d.fcmToken IS NOT NULL")
    List<String> findActiveFcmTokensByUserId(@Param("userId") Long userId);

    boolean existsByUser_IdAndLastCityIgnoreCase(Long userId, String lastCity);

    @Query("SELECT COUNT(d) > 0 FROM UserDeviceEntity d WHERE d.user.id = :userId AND d.lastIpAddress LIKE :subnetPattern")
    boolean existsByUser_IdAndSubnet(@Param("userId") Long userId, @Param("subnetPattern") String subnetPattern);

    boolean existsByUser_Id(Long userId);

}
