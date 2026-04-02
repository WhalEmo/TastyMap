package com.beem.TastyMap.Search;

import com.beem.TastyMap.Maps.Entity.PlaceEntity;
import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.Search.Data.GlobalSearchResult;
import jakarta.persistence.EntityManager;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class GlobalSearchService {

    private final EntityManager manager;

    public GlobalSearchService(EntityManager manager) {
        this.manager = manager;
    }

    public GlobalSearchResult searchEverything(String searchText, int page, int size){
        SearchSession searchSession = Search.session(manager);

        String queryText = searchText.trim();

        int offSet = page * size;

        SearchResult<Object> results = searchSession.search(Arrays.asList(PlaceEntity.class, UserEntity.class))
                .where(f -> f.bool()
                        .should(f.match()
                                .fields("name", "vicinity", "username", "surname")
                                .matching(queryText)
                                .fuzzy(1)
                        )
                        .should(f.wildcard()
                                .fields("name", "username")
                                .matching(queryText + "*")
                        )
                )
                .fetch(20);

        GlobalSearchResult globalSearchResult = new GlobalSearchResult();

        for(Object hit: results.hits()){
            if(hit instanceof PlaceEntity){
                globalSearchResult.getVenues().add(((PlaceEntity) hit).getName());
            }
            else if(hit instanceof UserEntity){
                globalSearchResult.getUsers().add(((UserEntity) hit).getUsername());
            }
        }
        return globalSearchResult;
    }
}
