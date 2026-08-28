package com.beem.TastyMap.userRelated.health.entitys;

import jakarta.persistence.*;

@Entity
@Table(name = "allergies") // Veritabanındaki tablo adıyla birebir aynı (küçük harf)
public class AllergiesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "allergy_name", unique = true) // DB'deki 'allergy_name' sütununa bağladık
    private String allergyName;

    public AllergiesEntity() {
    }

    public AllergiesEntity(Long id, String allergyName) {
        this.id = id;
        this.allergyName = allergyName;
    }

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