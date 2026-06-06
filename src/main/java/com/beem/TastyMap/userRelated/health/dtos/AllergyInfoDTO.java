package com.beem.TastyMap.userRelated.health.dtos;

public class AllergyInfoDTO {
    private Long id;
    private String name;

    public AllergyInfoDTO(Long id, String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
