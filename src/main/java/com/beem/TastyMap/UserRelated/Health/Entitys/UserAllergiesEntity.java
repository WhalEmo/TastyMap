package com.beem.TastyMap.UserRelated.Health.Entitys;

import com.beem.TastyMap.RegisterLogin.UserEntity;
import jakarta.persistence.*;
@Entity
@Table(
        name = "user_allergies",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_allergy", columnNames = {"user_id", "allergies_id"})
        },
        indexes = {
                @Index(name = "idx_user_allergies_user", columnList = "user_id"),
                @Index(name = "idx_user_allergies_allergy", columnList = "allergies_id")
        }
)
public class UserAllergiesEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allergies_id")
    private AllergiesEntity allergies;

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

    public AllergiesEntity getAllergies() {
        return allergies;
    }

    public void setAllergies(AllergiesEntity allergies) {
        this.allergies = allergies;
    }
}
