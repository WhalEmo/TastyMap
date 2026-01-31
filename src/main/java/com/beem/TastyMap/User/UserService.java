package com.beem.TastyMap.User;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepo userRepo;

    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Cacheable(value = "users", key = "#id")
    public User getUserById(Long id){
        return userRepo
                .findById(id)
                .orElseThrow(()-> new RuntimeException("User not found"));
    }
}
