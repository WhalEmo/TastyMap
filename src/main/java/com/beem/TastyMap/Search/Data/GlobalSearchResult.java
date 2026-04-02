package com.beem.TastyMap.Search.Data;


import java.util.ArrayList;
import java.util.List;

public class GlobalSearchResult {
    private List<String> venues = new ArrayList<>();
    private List<String> users = new ArrayList<>();

    public GlobalSearchResult() {
    }

    public List<String> getVenues() {
        return venues;
    }

    public void setVenues(List<String> venues) {
        this.venues = venues;
    }

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }
}
