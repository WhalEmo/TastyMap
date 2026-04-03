package com.beem.TastyMap.UserRelated.Post;

import com.beem.TastyMap.Exceptions.CustomExceptions;
import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.RegisterLogin.UserRepo;
import com.beem.TastyMap.UserRelated.Post.Like.PostLikeDTO;
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
    public PostResponseDTO addPost(PostAndVisitRequestDTO dto, Long myId){
        UserEntity userRef = entityManager.getReference(UserEntity.class, myId);

        var userView = userRepo.findUserProjectionById(myId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Kullanıcı bulunamadı."));

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
        return convertToResponseDTO(post,false,userView);
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
        PostEntity post = postRepo.findByIdWithUser(postId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Post bulunamadı"));
        if (!post.getUser().getId().equals(myId)) {
            throw new CustomExceptions.AuthorizationException("Bu postu güncelleme yetkin yok");
        }

        if (dto.getExplanation() != null&& !dto.getExplanation().equals(post.getExplanation())) {
            post.setExplanation(dto.getExplanation().trim());
        }
        if(!dto.getPhotoUrl().equals(post.getPhotoUrl())){
            post.setPhotoUrl(dto.getPhotoUrl());
        }
        if(!dto.getPuan().equals(post.getPuan())){
            post.setPuan(dto.getPuan());
        }
        postRepo.save(post);
        boolean isLiked = likeRepo.existsByPostIdAndUserId(postId, myId);
        return convertToResponseDTO(post, isLiked);
    }

    @Transactional
    public PostResponseDTO togglePinPost(Long postId, Long myId) {
        PostEntity post = postRepo.findByIdWithUser(postId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Post bulunamadı"));

        if (!post.getUser().getId().equals(myId)) {
            throw new CustomExceptions.AuthorizationException("Sadece kendi postunu sabitleyebilirsin");
        }
        if (post.isPinned()) {
            post.setPinned(false);
        } else {
            long currentPinnedCount = postRepo.countByUserIdAndIsPinnedTrue(myId);
            if (currentPinnedCount >= 3) {
                throw new CustomExceptions.BadRequestException("Maksimum 3 post sabitleyebilirsin.");
            }
            post.setPinned(true);
        }
        postRepo.save(post);
        boolean isLiked = likeRepo.existsByPostIdAndUserId(postId, myId);
        return convertToResponseDTO(post, isLiked);
    }

    @Transactional
    public PostLikeDTO toggleLike(Long postId, Long userId){
        var postView = postRepo.findStatsByCPostId(postId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Post bulunamadı"));

        accessChecker.checkAccess(postView.getOwnerId(),userId);
        Optional<Long> existingLike = likeRepo.findIdByPostIdAndUserId(postId, userId);
        if(existingLike.isPresent()){
            likeRepo.deleteById(existingLike.get());
            postRepo.decrementLike(postId);
            return new PostLikeDTO(false,postView.getNumberOfLikes()-1);
        }else{
            UserEntity userRef = entityManager.getReference(UserEntity.class, userId);
            PostEntity postRef = entityManager.getReference(PostEntity.class, postId);
            PostLikeEntity like=new PostLikeEntity();
            like.setPost(postRef);
            like.setUser(userRef);

            likeRepo.save(like);
            postRepo.incrementLike(postId);
            return new PostLikeDTO(true,postView.getNumberOfLikes()+1);
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

    private PostResponseDTO convertToResponseDTO(PostEntity post, boolean isLiked, UserRepo.UserProfileView userView) {
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
        dto.setLiked(isLiked);


        dto.setUserId(userView.getId());
        dto.setUsername(userView.getUsername());
        dto.setProfilePhotoUrl(userView.getProfile());

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

        return dto;
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
        dto.setLiked(isLiked);


        dto.setUserId(post.getUser().getId());
        dto.setUsername(post.getUser().getUsername());
        dto.setProfilePhotoUrl(post.getUser().getProfile());

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

        return dto;
    }

}





