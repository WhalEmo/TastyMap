package com.beem.TastyMap.UserRelated.Post;

import com.beem.TastyMap.Exceptions.CustomExceptions;
import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.UserRelated.Post.Like.PostLikeEntity;
import com.beem.TastyMap.UserRelated.Post.Like.PostLikeRepo;

import com.beem.TastyMap.UserRelated.Post.Like.PostLikeUserDTO;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PostService {
    private final PostRepo postRepo;
    private final AccessChecker accessChecker;
    private final EntityManager entityManager;
    private final PostLikeRepo likeRepo;


    public PostService(PostRepo postRepo, AccessChecker accessChecker, EntityManager entityManager, PostLikeRepo likeRepo) {
        this.postRepo = postRepo;
        this.accessChecker = accessChecker;
        this.entityManager = entityManager;
        this.likeRepo = likeRepo;
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
        if (dto.getExplanation() != null) {
            post.setExplanation(dto.getExplanation().trim());
        }
        post.setPuan(dto.getPuan());
        post.setUser(userRef);
        post.setPhotoUrl(dto.getPhotoUrl());
        post.setPlaceEmbedded(place);
        post.setCommentEnabled(dto.isCommentEnabled());
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
        if (dto.getExplanation() != null) {
            post.setExplanation(dto.getExplanation().trim());
        }
        post.setPuan(dto.getPuan());
        post.setPhotoUrl(dto.getPhotoUrl());

        postRepo.save(post);
    }

    @Transactional
    public String toggleLike(Long postId,Long userId){
        PostEntity post = postRepo.findById(postId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Post bulunamadı"));
        accessChecker.checkAccess(post.getUser().getId(),userId);
        Optional<PostLikeEntity> existingLike = likeRepo.findByPost_IdAndUser_Id(postId, userId);
        if(existingLike.isPresent()){
            likeRepo.delete(existingLike.get());
            postRepo.decrementLike(postId);
            return "Beğeni kaldırıldı.";

        }else{
            UserEntity userRef = entityManager.getReference(UserEntity.class, userId);
            PostLikeEntity like=new PostLikeEntity();
            like.setPost(post);
            like.setUser(userRef);
            likeRepo.saveAndFlush(like);
            postRepo.incrementLike(postId);
            return "Beğenildi.";
        }
    }

    @Transactional(readOnly = true)
    public Page<PostLikeUserDTO> whosLike(
            Long postId,
            Long myId,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return likeRepo.findPostLikesFullOrdered(postId, myId, pageable);
    }


}





