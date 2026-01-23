package com.beem.TastyMap.UserProfile.Post;

import com.beem.TastyMap.Exceptions.CustomExceptions;
import com.beem.TastyMap.RegisterLogin.UserRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {
    private final PostRepo postRepo;
    private final UserRepo userRepo;

    public PostService(PostRepo postRepo, UserRepo userRepo) {
        this.postRepo = postRepo;
        this.userRepo = userRepo;
    }
    @Transactional
    public void addPost(PostRequestDTO dto,Long myId){
        PlaceEmbedded place=new PlaceEmbedded();
        place.setPlaceId(dto.getPlaceId());
        place.setCity(dto.getCity());
        place.setCategories(dto.getCategories());
        place.setAveragePuan(dto.getAveragePuan());
        place.setDistrict(dto.getDistrict());
        place.setNeighbourhood(dto.getNeighbourhood());
        place.setPlaceName(dto.getPlaceName());
        place.setLatitude(dto.getLatitude());
        place.setLongitude(dto.getLongitude());

        PostEntity post=new PostEntity();
        post.setExplanation(dto.getExplanation());
        post.setPuan(dto.getPuan());
        post.setUserId(myId);
        post.setPhotoUrl(dto.getPhotoUrl());
        post.setPlaceEmbedded(place);
        postRepo.save(post);
    }

    public void getPosts(){

    }
}
