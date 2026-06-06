package com.beem.TastyMap.search.data;


import java.util.ArrayList;
import java.util.List;

public class GlobalSearchResult {
    private List<VenueResult> venues = new ArrayList<>();
    private List<AppUserResult> users = new ArrayList<>();

    public GlobalSearchResult() {
    }

    public List<VenueResult> getVenues() {
        return venues;
    }

    public void setVenues(List<VenueResult> venues) {
        this.venues = venues;
    }

    public List<AppUserResult> getUsers() {
        return users;
    }

    public void setUsers(List<AppUserResult> users) {
        this.users = users;
    }
}
