package com.beem.TastyMap.UserProfile.Post;

import com.beem.TastyMap.Exceptions.CustomExceptions;
import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.RegisterLogin.UserRepo;
import com.beem.TastyMap.UserProfile.Block.BlockRepo;
import com.beem.TastyMap.UserProfile.Subscribe.SubscribeRepo;
import com.beem.TastyMap.UserProfile.Subscribe.SubscribeService;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {
    private final PostRepo postRepo;
    private final AccessChecker accessChecker;
    private final EntityManager entityManager;


    public PostService(PostRepo postRepo, AccessChecker accessChecker, EntityManager entityManager) {
        this.postRepo = postRepo;
        this.accessChecker = accessChecker;
        this.entityManager = entityManager;
    }

    @Transactional
    public void addPost(PostRequestDTO dto,Long myId){
        UserEntity userRef = entityManager.getReference(UserEntity.class, myId);

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
        post.setExplanation(dto.getExplanation().trim());
        post.setPuan(dto.getPuan());
        post.setUser(userRef);
        post.setPhotoUrl(dto.getPhotoUrl());
        post.setPlaceEmbedded(place);
        postRepo.save(post);
    }

    public Page<PostResponseDTO> getPosts(Long userId, Long myId, int page, int size) {
        accessChecker.checkAccess(userId,myId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepo.getUserPosts(userId, pageable);
    }

    public void deletePost(Long postId,Long myId){
        PostEntity post=postRepo.findById(postId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Post bulunamadı"));
        if (!post.getUser().getId().equals(myId)) {
            throw new CustomExceptions.AuthorizationException("Bu postu silme yetkin yok");
        }
        postRepo.delete(post);
    }

    @Transactional
    public void updatePost(Long postId, Long myId, PostUpdateDTO dto) {

        PostEntity post = postRepo.findById(postId)
                .orElseThrow(() ->
                        new CustomExceptions.NotFoundException("Post bulunamadı")
                );

        if (!post.getUser().getId().equals(myId)) {
            throw new CustomExceptions.AuthorizationException("Bu postu güncelleme yetkin yok");
        }
        post.setExplanation(dto.getExplanation().trim());
        post.setPuan(dto.getPuan());
        post.setPhotoUrl(dto.getPhotoUrl());

        postRepo.save(post);
    }


}










