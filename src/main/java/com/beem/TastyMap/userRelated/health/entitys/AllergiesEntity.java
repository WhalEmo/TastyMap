package com.beem.TastyMap.userRelated.health.entitys;

import jakarta.persistence.*;

@Entity
@Table(name = "Allergies",uniqueConstraints = {@UniqueConstraint(columnNames = "allergyName")})
public class AllergiesEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String allergyName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAllergyName() {
        return allergyName;
    }

    public void setAllergyName(String allergyName) {
        this.allergyName = allergyName;
    }
}
