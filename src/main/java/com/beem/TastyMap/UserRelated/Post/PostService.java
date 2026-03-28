package com.beem.TastyMap.UserRelated.Post;

import com.beem.TastyMap.Exceptions.CustomExceptions;
import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.RegisterLogin.UserRepo;
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
    private final UserRepo userRepo;
    private final AccessChecker accessChecker;
    private final EntityManager entityManager;
    private final PostLikeRepo likeRepo;


    public PostService(PostRepo postRepo, UserRepo userRepo, AccessChecker accessChecker, EntityManager entityManager, PostLikeRepo likeRepo) {
        this.postRepo = postRepo;
        this.userRepo = userRepo;
        this.accessChecker = accessChecker;
        this.entityManager = entityManager;
        this.likeRepo = likeRepo;
    }

    @Transactional
    public PostResponseDTO addPost(PostRequestDTO dto,Long myId){
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
        userRepo.updatePostCount(userRef.getId(),1);
        return convertToResponseDTO(post,false);
    }

    public Page<PostResponseDTO> getPosts(Long userId, Long myId, int page, int size) {
        accessChecker.checkAccess(userId,myId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepo.getUserPosts(userId, myId, pageable);
    }

    @Transactional
    public void deletePost(Long postId, Long myId) {
        PostRepo.PostStatusView postStatus = postRepo.findPostStatusById(postId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Post bulunamadı"));
        if (!postStatus.getAuthorId().equals(myId)) {
            throw new CustomExceptions.AuthorizationException("Bu postu silme yetkin yok");
        }
        postRepo.deleteById(postId);
        userRepo.updatePostCount(myId, -1);
    }

    @Transactional
    public PostResponseDTO updatePost(Long postId, Long myId, PostUpdateDTO dto) {
        PostEntity post = postRepo.findById(postId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Post bulunamadı"));
        if (!post.getUser().getId().equals(myId)) {
            throw new CustomExceptions.AuthorizationException("Bu postu güncelleme yetkin yok");
        }
        if (dto.getExplanation() != null) post.setExplanation(dto.getExplanation().trim());
        post.setPuan(dto.getPuan());
        post.setPhotoUrl(dto.getPhotoUrl());
        postRepo.save(post);
        boolean isLiked = likeRepo.existsByPostIdAndUserId(postId, myId);
        return convertToResponseDTO(post, isLiked);
    }

    @Transactional
    public PostResponseDTO togglePinPost(Long postId, Long myId) {
        PostEntity post = postRepo.findById(postId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Post bulunamadı"));

        if (!post.getUser().getId().equals(myId)) {
            throw new CustomExceptions.AuthorizationException("Sadece kendi postunu sabitleyebilirsin");
        }
        if (post.isPinned()) {
            post.setPinned(false);
        } else {
            long currentPinnedCount = postRepo.countByUserIdAndIsPinnedTrue(myId);
            if (currentPinnedCount >= 5) {
                throw new CustomExceptions.BadRequestException("Maksimum 3 post sabitleyebilirsin.");
            }
            post.setPinned(true);
        }
        postRepo.save(post);
        boolean isLiked = likeRepo.existsByPostIdAndUserId(postId, myId);
        return convertToResponseDTO(post, isLiked);
    }

    @Transactional
    public String toggleLike(Long postId,Long userId){
        Long postUserId = postRepo.findOwnerIdByPostId(postId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Post bulunamadı"));

        accessChecker.checkAccess(postUserId,userId);
        Optional<Long> existingLike = likeRepo.findIdByPostIdAndUserId(postId, userId);
        if(existingLike.isPresent()){
            likeRepo.deleteById(existingLike.get());
            postRepo.decrementLike(postId);
            return "Beğeni kaldırıldı.";
        }else{
            UserEntity userRef = entityManager.getReference(UserEntity.class, userId);
            PostEntity postRef = entityManager.getReference(PostEntity.class, postId);
            PostLikeEntity like=new PostLikeEntity();
            like.setPost(postRef);
            like.setUser(userRef);

            likeRepo.save(like);
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

    private PostResponseDTO convertToResponseDTO(PostEntity post, boolean isLiked) {
        PostResponseDTO dto = new PostResponseDTO();
        dto.setPostId(post.getId());
        dto.setExplanation(post.getExplanation());
        dto.setPuan(post.getPuan());
        dto.setPhotoUrl(post.getPhotoUrl());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setCommentEnabled(post.isCommentEnabled());
        dto.setNumberof_likes(post.getNumberofLikes());
        dto.setCommentCount(post.getCommentCount());
        dto.setPinned(post.isPinned());

        if (post.getUser() != null) {
            dto.setUserId(post.getUser().getId());
            dto.setUsername(post.getUser().getUsername());
            dto.setProfilePhotoUrl(post.getUser().getProfile());
        }
        if (post.getPlaceEmbedded() != null) {
            PlaceEmbedded place = post.getPlaceEmbedded();
            dto.setPlaceId(place.getPlaceId());
            dto.setPlaceName(place.getPlaceName());
            dto.setCategories(place.getCategories());
            dto.setCity(place.getCity());
            dto.setDistrict(place.getDistrict());
            dto.setNeighbourhood(place.getNeighbourhood());
            dto.setLatitude(place.getLatitude());
            dto.setLongitude(place.getLongitude());
            dto.setAveragePuan(place.getAveragePuan());
        }
        dto.setLiked(isLiked);

        return dto;
    }
}





