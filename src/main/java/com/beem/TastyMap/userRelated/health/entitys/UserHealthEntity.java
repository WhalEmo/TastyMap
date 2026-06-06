package com.beem.TastyMap.userRelated.health.entitys;

import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.userRelated.health.HealthEnum;
import jakarta.persistence.*;

@Entity
@Table(name = "user_healthies")
public class UserHealthEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private UserEntity user;

    @Column(name = "diabetes",nullable = false)
    private boolean hasDiabetes=false;

    @Column(name = "eat_type",nullable = false)
    private HealthEnum eatType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public boolean isHasDiabetes() {
        return hasDiabetes;
    }

    public void setHasDiabetes(boolean hasDiabetes) {
        this.hasDiabetes = hasDiabetes;
    }

    public HealthEnum getEatType() {
        return eatType;
    }

    public void setEatType(HealthEnum eatType) {
        this.eatType = eatType;
    }
}
