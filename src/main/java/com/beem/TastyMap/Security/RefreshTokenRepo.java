package com.beem.TastyMap.Security;

import com.beem.TastyMap.RegisterLogin.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepo extends JpaRepository<RefreshTokenEntity,Long> {
}
