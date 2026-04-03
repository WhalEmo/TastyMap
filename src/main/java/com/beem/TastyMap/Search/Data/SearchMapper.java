package com.beem.TastyMap.Search.Data;

import com.beem.TastyMap.Maps.Entity.PlaceEntity;
import com.beem.TastyMap.RegisterLogin.UserEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SearchMapper {

    public VenueResult toVenueResult(PlaceEntity entity){
        return new VenueResult(
                entity.getId(),
                entity.getName(),
                entity.getVicinity()
        );
    }

    public AppUserResult toAppUserResult(UserEntity entity){
        return new AppUserResult(
                entity.getId(),
                entity.getUsername(),
                entity.getBiography(),
                entity.getProfile()
        );
    }

    public GlobalSearchResult toGlobalSearchResult(List<Object> hits){
        GlobalSearchResult globalSearchResult = new GlobalSearchResult();

        for(Object hit: hits){
            if(hit instanceof VenueResult){
                globalSearchResult.getVenues().add(
                        (VenueResult) hit
                );
            }
            else if(hit instanceof AppUserResult){
                globalSearchResult.getUsers().add(
                        (AppUserResult) hit
                );
            }
        }

        return globalSearchResult;
    }
}
