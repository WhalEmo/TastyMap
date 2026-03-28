package com.beem.TastyMap.RegisterLogin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface UserRepo extends JpaRepository<UserEntity,Long> {
    Optional<UserEntity>findByUsername(String username);
    Optional<UserEntity>findByEmail(String email);
    boolean existsByUsernameAndIdNot(String username,Long Id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

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
}
