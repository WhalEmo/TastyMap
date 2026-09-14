package com.beem.TastyMap.user.account.repo;

import com.beem.TastyMap.user.account.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;


public interface UserRepo extends JpaRepository<UserEntity,Long> {
    Optional<UserEntity>findByEmail(String email);
    boolean existsByUsernameAndIdNot(String username,Long Id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT u.id as id, u.username as username, u.profile as profile FROM UserEntity u WHERE u.id = :id")
    Optional<UserProfileView> findUserProjectionById(@Param("id") Long id);
    interface UserProfileView {
        Long getId();
        String getUsername();
        String getProfile();
    }

    @Query("SELECT u.privateProfile FROM UserEntity u WHERE u.id = :userId")
    Optional<Boolean> isProfilePrivate(@Param("userId") Long userId);


    @Modifying
    @Query("UPDATE UserEntity u SET u.postCount = u.postCount + :amount WHERE u.id = :userId")
    void updatePostCount(@Param("userId") Long userId, @Param("amount") int amount);

    @Modifying
    @Query("UPDATE UserEntity u SET u.subscriberCount = u.subscriberCount + :amount WHERE u.id = :userId")
    void updateSubscriberCount(@Param("userId") Long userId, @Param("amount") int amount);

    @Modifying
    @Query("UPDATE UserEntity u SET u.subscribedCount = u.subscribedCount + :amount WHERE u.id = :userId")
    void updateSubscribedCount(@Param("userId") Long userId, @Param("amount") int amount);

    boolean existsByIdAndEmailVerifiedTrue(Long userId);

    @Query(value = "SELECT * FROM users WHERE username = :username", nativeQuery = true)
    Optional<UserEntity> findByUsernameIncludingDeleted(@Param("username") String username);

    // 30 günü doldurmuş silinmiş hesapları kalıcı olarak temizler
    @Modifying
    @Query(value = "DELETE FROM users WHERE is_deleted = true AND deleted_at < :dateThreshold", nativeQuery = true)
    int hardDeleteExpiredAccounts(@Param("dateThreshold") LocalDateTime dateThreshold);


}
