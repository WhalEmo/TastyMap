package com.beem.TastyMap.post.service;

import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.file.FileStorageService;
import com.beem.TastyMap.post.dto.PostAndVisitRequestDTO;
import com.beem.TastyMap.post.dto.PostGridResponseDTO;
import com.beem.TastyMap.post.dto.PostResponseDTO;
import com.beem.TastyMap.post.dto.PostUpdateDTO;
import com.beem.TastyMap.post.entity.PlaceEmbedded;
import com.beem.TastyMap.post.entity.PostEntity;
import com.beem.TastyMap.post.like.PostLikeDTO;
import com.beem.TastyMap.post.like.PostLikeEntity;
import com.beem.TastyMap.post.like.PostLikeRepo;
import com.beem.TastyMap.post.like.PostLikeUserDTO;
import com.beem.TastyMap.post.repo.PostRepo;
import com.beem.TastyMap.user.account.entity.UserEntity;
import com.beem.TastyMap.user.account.repo.UserRepo;
import jakarta.persistence.EntityManager;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
public class PostService {
    private final PostRepo postRepo;
    private final UserRepo userRepo;
    private final AccessChecker accessChecker;
    private final EntityManager entityManager;
    private final FileStorageService fileStorageService;
    private final PostLikeRepo likeRepo;
    private final MessageSource messageSource;

    public PostService(PostRepo postRepo,
                       UserRepo userRepo,
                       AccessChecker accessChecker,
                       EntityManager entityManager, FileStorageService fileStorageService,
                       PostLikeRepo likeRepo,
                       MessageSource messageSource) {
        this.postRepo = postRepo;
        this.userRepo = userRepo;
        this.accessChecker = accessChecker;
        this.entityManager = entityManager;
        this.fileStorageService = fileStorageService;
        this.likeRepo = likeRepo;
        this.messageSource = messageSource;
    }

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    @Transactional
    public PostResponseDTO addPost(PostAndVisitRequestDTO dto, Long myId) {
        if (dto.getPhotoUrl() != null && dto.getPhotoUrl().size() > 3) {
            throw new CustomExceptions.BadRequestException(getMessage("post.photo.limit.exceeded"));
        }
        UserEntity userRef = entityManager.getReference(UserEntity.class, myId);

        var userView = userRepo.findUserProjectionById(myId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException(getMessage("user.not.found.simple")));

        PlaceEmbedded place = new PlaceEmbedded();
        place.setPlaceId(dto.getPlaceId());
        place.setCity(dto.getCity());
        place.setCategories(dto.getCategories());
        place.setAveragePuan(dto.getAveragePoint());
        place.setDistrict(dto.getDistrict());
        place.setNeighbourhood(dto.getNeighbourhood());
        place.setPlaceName(dto.getPlaceName());
        place.setLatitude(dto.getLatitude());
        place.setLongitude(dto.getLongitude());

        PostEntity post = new PostEntity();
        if (dto.getExplanation() != null) {
            post.setExplanation(dto.getExplanation().trim());
        }
        post.setUser(userRef);
        post.setPhotoUrls(dto.getPhotoUrl());
        post.setPlaceEmbedded(place);
        post.setCommentEnabled(dto.isCommentEnabled());
        postRepo.save(post);
        userRepo.updatePostCount(userRef.getId(), 1);
        return convertToResponseDTO(post, false, userView);
    }

    @Transactional(readOnly = true)
    public Page<PostGridResponseDTO> getUserGridPosts(Long targetUserId, Long myId, int page, int size) {
        if(!Objects.equals(targetUserId, myId)) {
            accessChecker.checkAccess(targetUserId, myId);
        }
        Pageable pageable = PageRequest.of(page, size);
        return postRepo.findUserGridPosts(targetUserId, pageable);
    }

    @Transactional(readOnly = true)
    public PostResponseDTO getPostDetail(Long postId, Long myId) {
        PostEntity post = postRepo.findByIdWithUser(postId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException(getMessage("post.not.found")));

        if(!Objects.equals(post.getUser().getId(),myId)){
            accessChecker.checkAccess(post.getUser().getId(), myId);
        }
        boolean isLiked = likeRepo.existsByPostIdAndUserId(postId, myId);

        System.out.println("POST_ID: " + postId + " | MY_ID: " + myId + " | IS_LIKED: " + isLiked);

        return convertToResponseDTO(post, isLiked);
    }

    @Transactional
    public void deletePost(Long postId, Long myId) {
        PostEntity post = postRepo.findById(postId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException(getMessage("post.not.found")));

        if (!post.getUser().getId().equals(myId)) {
            throw new CustomExceptions.AuthorizationException(getMessage("post.delete.unauthorized"));
        }

        if (post.getPhotoUrls() != null && !post.getPhotoUrls().isEmpty()) {
            fileStorageService.deleteFilesByUrls(post.getPhotoUrls());
        }

        postRepo.delete(post);
        userRepo.updatePostCount(myId, -1);
    }

    @Transactional
    public PostResponseDTO updatePost(Long postId, Long myId, PostUpdateDTO dto) {
        PostEntity post = postRepo.findByIdWithUser(postId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException(getMessage("post.not.found")));
        if (!post.getUser().getId().equals(myId)) {
            throw new CustomExceptions.AuthorizationException(getMessage("post.update.unauthorized"));
        }
        if (dto.getExplanation() != null && !dto.getExplanation().equals(post.getExplanation())) {
            post.setExplanation(dto.getExplanation().trim());
        }
        if (!dto.getPhotoUrl().equals(post.getPhotoUrls())) {
            post.setPhotoUrls(dto.getPhotoUrl());
        }
        post.setUpdateDate(LocalDateTime.now());
        postRepo.save(post);
        boolean isLiked = likeRepo.existsByPostIdAndUserId(postId, myId);
        return convertToResponseDTO(post, isLiked);
    }

    @Transactional
    public PostResponseDTO togglePinPost(Long postId, Long myId) {
        PostEntity post = postRepo.findByIdWithUser(postId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException(getMessage("post.not.found")));

        if (!post.getUser().getId().equals(myId)) {
            throw new CustomExceptions.AuthorizationException(getMessage("post.pin.unauthorized"));
        }
        if (post.isPinned()) {
            post.setPinned(false);
        } else {
            long currentPinnedCount = postRepo.countByUserIdAndIsPinnedTrue(myId);
            if (currentPinnedCount >= 3) {
                throw new CustomExceptions.BadRequestException(getMessage("post.pin.limit.exceeded"));
            }
            post.setPinned(true);
        }
        postRepo.save(post);
        boolean isLiked = likeRepo.existsByPostIdAndUserId(postId, myId);
        return convertToResponseDTO(post, isLiked);
    }

    @Transactional
    public PostLikeDTO toggleLike(Long postId, Long userId) {
        boolean postExists = postRepo.existsById(postId);
        if (!postExists) {
            throw new CustomExceptions.NotFoundException(getMessage("post.not.found"));
        }

        Optional<Long> existingLike = likeRepo.findIdByPostIdAndUserId(postId, userId);
        boolean isLiked;

        if (existingLike.isPresent()) {
            likeRepo.deleteById(existingLike.get());
            postRepo.decrementLike(postId);
            isLiked = false;
        } else {
            UserEntity userRef = entityManager.getReference(UserEntity.class, userId);
            PostEntity postRef = entityManager.getReference(PostEntity.class, postId);

            PostLikeEntity like = new PostLikeEntity();
            like.setPost(postRef);
            like.setUser(userRef);

            likeRepo.save(like);
            postRepo.incrementLike(postId);
            isLiked = true;
        }

        // Güncelleme sonrası veritabanındaki kesin beğeni sayısını sorgula
        int updatedLikes = postRepo.findNumberOfLikesByPostId(postId).orElse(0);

        return new PostLikeDTO(isLiked, updatedLikes);
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
        dto.setPhotoUrl(post.getPhotoUrls());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setCommentEnabled(post.isCommentEnabled());
        dto.setLikeCount(post.getNumberofLikes());
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
            dto.setAveragePoint(place.getAveragePuan());
        }

        return dto;
    }

    private PostResponseDTO convertToResponseDTO(PostEntity post, boolean isLiked) {
        PostResponseDTO dto = new PostResponseDTO();
        dto.setPostId(post.getId());
        dto.setExplanation(post.getExplanation());
        dto.setPhotoUrl(post.getPhotoUrls());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdateDate(post.getUpdateDate());
        dto.setCommentEnabled(post.isCommentEnabled());
        dto.setLikeCount(post.getNumberofLikes());
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
            dto.setAveragePoint(place.getAveragePuan());
        }
        return dto;
    }
}