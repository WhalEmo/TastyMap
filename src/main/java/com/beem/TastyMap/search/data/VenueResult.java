package com.beem.TastyMap.search.data;

public class VenueResult {

    private Long id;
    private String name;
    private String vicinity;


    public VenueResult() {
    }

    public VenueResult(Long id, String name, String vicinity) {
        this.id = id;
        this.name = name;
        this.vicinity = vicinity;
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

    public String getVicinity() {
        return vicinity;
    }

    public void setVicinity(String vicinity) {
        this.vicinity = vicinity;
    }
}
