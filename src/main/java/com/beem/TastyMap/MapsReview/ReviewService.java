package com.beem.TastyMap.MapsReview;

import com.beem.TastyMap.Exceptions.CustomExceptions;
import com.beem.TastyMap.Maps.Entity.PlaceEntity;
import com.beem.TastyMap.Maps.Service.PlacesService;
import com.beem.TastyMap.MapsReview.Data.Request.SentReviewReq;
import com.beem.TastyMap.MapsReview.Data.Request.UpdateReviewReq;
import com.beem.TastyMap.MapsReview.Data.Response.CreatedReviewRes;
import com.beem.TastyMap.MapsReview.Data.Response.UpdatedReviewRes;
import com.beem.TastyMap.MapsReview.Data.Response.ReviewResponse;
import com.beem.TastyMap.MapsReview.Data.ReviewMapper;
import com.beem.TastyMap.MapsReview.Data.ReviewResult;
import com.beem.TastyMap.MapsReview.Data.ScoreDto;
import com.beem.TastyMap.MapsReview.Entity.ReviewEntity;
import com.beem.TastyMap.MapsReview.Entity.ScoreEntity;
import com.beem.TastyMap.MapsReview.Enum.ReviewSource;
import com.beem.TastyMap.MapsReview.Enum.ReviewStatus;
import com.beem.TastyMap.MapsReview.Enum.ScoreType;
import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.RegisterLogin.UserRepo;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class ReviewService {

    private final ReviewRepo reviewRepo;
    private final PlacesService placesService;
    private final EntityManager entityManager;
    private final UserRepo userRepo;
    private final ReviewMapper reviewMapper;

    public ReviewService(ReviewRepo reviewRepo, PlacesService placesService,
                         EntityManager entityManager, UserRepo userRepo, ReviewMapper reviewMapper) {
        this.reviewRepo = reviewRepo;
        this.placesService = placesService;
        this.entityManager = entityManager;
        this.userRepo = userRepo;
        this.reviewMapper = reviewMapper;
    }

    public ReviewResponse getPlaceReviews(String placeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        List<ReviewResult> entityList = reviewRepo
                .findByPlace_PlaceId(placeId, pageable)
                .getContent()
                .stream()
                .map(ReviewResult::fromEntity)
                .toList();

        return new ReviewResponse(
                page,
                size,
                entityList,
                placeId
        );
    }


    private ReviewEntity getParentReviewReference(Long parentId) {
        if(parentId == null) {
            return null;
        }
        if(!reviewRepo.existsById(parentId)) {
            throw new RuntimeException("Parent review not found");
        }
        return entityManager.getReference(ReviewEntity.class, parentId);
    }

    private ReviewEntity getReviewEntity(Long id){
        return reviewRepo
                .findById(id)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
    }

    private ReviewEntity getReviewByUserIdAndReviewId(Long userId, Long reviewId){
        return reviewRepo
                .findByIdAndUserId(reviewId, userId)
                .orElseThrow(()-> new CustomExceptions.NotFoundException("Review or user not found"));
    }

    @Transactional
    public CreatedReviewRes sendPlaceReview(SentReviewReq request, Long userId){
        createReviewRequestDtoControl(request);
        PlaceEntity place = placesService.getReferenceIfExists(request.getPlaceId());

        UserEntity user = userRepo.findById(userId)
                .orElseThrow(()-> new CustomExceptions.NotFoundException("User not found"));


        ReviewEntity entity = new ReviewEntity(
                user.getUsername(),
                request.getContent(),
                ReviewSource.INTERNAL,
                place,
                user,
                this.getParentReviewReference(
                        request.getParentId()
                ),
                ReviewStatus.APPROVED
        );

        applyScoreListCreate(request, entity);

        reviewRepo.saveAndFlush(entity);

        return new CreatedReviewRes(
                entity.getId(),
                place.getId(),
                entity.getAuthorName(),
                entity.getStatus(),
                entity.getCreatedAt(),
                request.getScores()
        );
    }

    @Transactional
    public Map<String, Object> deletePlaceReview(Long reviewId, Long userId){
        ReviewEntity review =
                getReviewByUserIdAndReviewId(userId, reviewId);
        if(review.getStatus() != ReviewStatus.APPROVED) throw new CustomExceptions.InvalidException("Review is not APPROVED.");
        review.setStatus(ReviewStatus.REJECTED);

        reviewRepo.rejectChildReviews(reviewId, ReviewStatus.REJECTED);
        reviewRepo.save(review);
        return reviewMapper.toDeletedReviewRes();
    }

    @Transactional
    public UpdatedReviewRes patchPlaceReview(UpdateReviewReq request, Long userId){
        ReviewEntity review =
                getReviewByUserIdAndReviewId(userId, request.getReviewId());

        //Security
        if(review.getStatus() != ReviewStatus.APPROVED)
            throw new CustomExceptions.ServiceException("This review not APPROVED.");
        updateScoreRatingControl(request);

        //Apply
        applyScoreUpdates(review, request);
        applyContentUpdate(review, request);
        applyScoreDelete(review, request);

        //Rebuild
        review.recalculateRating();

        ReviewEntity updatedReview = reviewRepo.save(review);
        return reviewMapper.toUpdatedReviewRes(updatedReview);
    }


    private void applyScoreUpdates(ReviewEntity review, UpdateReviewReq request) {
        if (request.getScores() == null) return;
        Map<ScoreType, ScoreEntity> existing =
                review.getScores().stream()
                        .collect(Collectors.toMap(
                                ScoreEntity::getType,
                                Function.identity()
                        ));

        for (ScoreDto sr : request.getScores()) {

            ScoreEntity score = existing.get(sr.getType());

            if (score != null) {
                score.setScore(sr.getScore());
            } else {
                review.getScores().add(new ScoreEntity(
                        sr.getType(),
                        sr.getScore(),
                        review
                ));
            }
        }
    }

    private void applyScoreDelete(ReviewEntity review, UpdateReviewReq request){
        if (request.getScores() == null) return;
        Set<ScoreType> incomingTypes =
                request.getScores().stream()
                        .map(ScoreDto::getType)
                        .collect(Collectors.toSet());

        review.getScores().removeIf(
                score -> !incomingTypes.contains(score.getType())
        );
    }

    private void applyContentUpdate(ReviewEntity review, UpdateReviewReq request) {

        if (request.getContent() == null || request.getContent().isEmpty()) {
            return;
        }

        review.setText(request.getContent());
    }
    private void updateScoreRatingControl(UpdateReviewReq req){
        if(req.getScores() == null) return;
        for(ScoreDto score: req.getScores()){
            if(score.getScore()<=0 || score.getScore()>5) throw new CustomExceptions.ServiceException("Score not between 0 and 5");
        }
    }

    private void createReviewRequestDtoControl(SentReviewReq request){
        if(request.getParentId() != null){
            if(
                    !reviewRepo.existsByIdAndPlaceIdAndSourceAndStatus(request.getParentId(),
                            request.getPlaceId(),
                            ReviewSource.INTERNAL,
                            ReviewStatus.APPROVED
                    )
            ){
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Parent review must be an approved internal review of this place"
                );
            }
            if(request.getScores() != null){
                throw new CustomExceptions.ServiceException("Child review haven't scores.");
            }
        }
    }

    private void applyScoreListCreate(SentReviewReq request, ReviewEntity entity){
        if(request.getParentId() != null && request.getScores() != null && !request.getScores().isEmpty()){
            throw new CustomExceptions.ServiceException("Child review have a scores.");
        }
        if(request.getParentId() == null && (request.getScores() == null || request.getScores().isEmpty())){
            throw new CustomExceptions.ServiceException("Review haven't scores.");
        }
        if(request.getScores() == null) return;
        if(request.getScores() != null && request.getScores().isEmpty()) return;
        if(request.getParentId() != null) return;

        List<ScoreEntity> scores = request
                .getScores()
                .stream()
                .map(scoreReq->{
                    return new ScoreEntity(
                            scoreReq.getType(),
                            scoreReq.getScore(),
                            entity
                    );
                })
                .toList();
        entity.setScores(scores);
    }

}
