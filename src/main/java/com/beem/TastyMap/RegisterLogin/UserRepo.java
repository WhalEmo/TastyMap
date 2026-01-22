package com.beem.TastyMap.RegisterLogin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepo extends JpaRepository<UserEntity,Long> {
    Optional<UserEntity>findByUsername(String username);
    Optional<UserEntity>findByEmail(String email);
    boolean existsByUsernameAndIdNot(String username,Long Id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
