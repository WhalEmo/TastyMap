package com.beem.TastyMap.search;


import com.beem.TastyMap.maps.entity.PlaceEntity;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.search.data.AppUserResult;
import com.beem.TastyMap.search.data.GlobalSearchResult;
import com.beem.TastyMap.search.data.SearchMapper;
import com.beem.TastyMap.search.data.VenueResult;
import jakarta.persistence.EntityManager;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class GlobalSearchService {

    private final EntityManager manager;
    private final SearchMapper mapper;

    public GlobalSearchService(EntityManager manager, SearchMapper mapper) {
        this.manager = manager;
        this.mapper = mapper;
    }

    public GlobalSearchResult searchEverything(String searchText, int page, int size){
        SearchSession searchSession = Search.session(manager);

        String queryText = searchText.trim();

        int offSet = page * size;

        SearchResult<Object> results = searchSession.search(Arrays.asList(PlaceEntity.class, UserEntity.class))
                .select(f -> f.<Object>composite(
                        projection -> {
                            String username = (String) projection.get(2);

                            if (username != null) {
                                return new AppUserResult(
                                        (Long) projection.get(0),
                                        username,
                                        (String) projection.get(3),
                                        (String) projection.get(5)
                                );
                            } else {
                                return new VenueResult(
                                        (Long) projection.get(0),
                                        (String) projection.get(6),
                                        (String) projection.get(1),
                                        (String) projection.get(4),
                                        (Double) projection.get(7),
                                        (Double) projection.get(8),
                                        (Double) projection.get(9),
                                        (Double) projection.get(10)
                                );
                            }
                        },
                        f.field("id", Long.class),              // 0
                        f.field("name", String.class),          // 1
                        f.field("username", String.class),      // 2
                        f.field("biography", String.class),     // 3
                        f.field("vicinity", String.class),      // 4
                        f.field("profile", String.class),       // 5
                        f.field("placeId", String.class),       // 6
                        f.field("latitude", Double.class),      // 7
                        f.field("longitude", Double.class),     // 8
                        f.field("googleRating", Double.class),  // 9
                        f.field("tastyMapRating", Double.class) // 10
                ))
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
                .fetch(
                        offSet,
                        size
                );

        return mapper.toGlobalSearchResult(results.hits());
    }
}
