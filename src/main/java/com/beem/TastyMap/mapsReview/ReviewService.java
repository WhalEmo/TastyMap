package com.beem.TastyMap.mapsReview;

import com.beem.TastyMap.MapsReview.ReviewUpdateEvent;
import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.maps.entity.PlaceEntity;
import com.beem.TastyMap.maps.service.PlacesService;
import com.beem.TastyMap.mapsReview.data.ReviewMapper;
import com.beem.TastyMap.mapsReview.data.ScoreDto;
import com.beem.TastyMap.mapsReview.data.request.SentReviewReq;
import com.beem.TastyMap.mapsReview.data.request.UpdateReviewReq;
import com.beem.TastyMap.mapsReview.data.response.CreatedReviewRes;
import com.beem.TastyMap.mapsReview.data.response.ReviewResponse;
import com.beem.TastyMap.mapsReview.data.ReviewResult;
import com.beem.TastyMap.mapsReview.data.response.UpdatedReviewRes;
import com.beem.TastyMap.mapsReview.entity.ReviewEntity;
import com.beem.TastyMap.mapsReview.entity.ScoreEntity;
import com.beem.TastyMap.mapsReview.enums.ReviewSource;
import com.beem.TastyMap.mapsReview.enums.ReviewStatus;
import com.beem.TastyMap.mapsReview.enums.ScoreType;
import com.beem.TastyMap.redis.RedisKeyGenerator;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.registerLogin.UserRepo;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    private final ApplicationEventPublisher eventPublisher;

    public ReviewService(ReviewRepo reviewRepo, PlacesService placesService,
                         EntityManager entityManager, UserRepo userRepo, ReviewMapper reviewMapper,
                         ApplicationEventPublisher eventPublisher) {
        this.reviewRepo = reviewRepo;
        this.placesService = placesService;
        this.entityManager = entityManager;
        this.userRepo = userRepo;
        this.reviewMapper = reviewMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ReviewResponse getPlaceReviews(String placeId, int page, int size) {
        boolean hasData = reviewRepo.existsByPlace_PlaceIdAndSource(placeId, ReviewSource.GOOGLE);

        if(hasData){
            eventPublisher.publishEvent(new ReviewUpdateEvent(placeId));
        }
        else{
            String key = RedisKeyGenerator.createPlaceDetailsKey(placeId);
            System.out.println("getPlaceReviews");
            placesService.searchPlaceDetailsGoogleAPI(
                    placeId,
                    key
            );
        }

        return getReviewDataBaseToResponse(
                placeId,
                page,
                size
        );
    }

    private ReviewResponse getReviewDataBaseToResponse(String placeId, int page, int size){
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        List<ReviewResult> reviewResults = reviewRepo
                .findAllByPlaceId(placeId, pageable)
                .stream()
                .map(ReviewResult::fromEntity)
                .toList();
        return new ReviewResponse(
                page,
                size,
                reviewResults,
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
        updateChildParentScoreControl(review, request);

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

    private void updateChildParentScoreControl(ReviewEntity entity, UpdateReviewReq request){
        boolean isChildReview = entity.getParent() != null;
        boolean hasScores = request.getScores() != null && !request.getScores().isEmpty();

        if(isChildReview && hasScores){
            throw new CustomExceptions.ServiceException(
                    "Child reviews cannot have scores."
            );
        }

        if(!isChildReview && !hasScores){
            throw new CustomExceptions.ServiceException(
                    "Parent reviews must have scores."
            );
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

        boolean isChildReview = request.getParentId() != null;
        boolean hasScores = request.getScores() != null && !request.getScores().isEmpty();

        if(isChildReview && hasScores){
            throw new CustomExceptions.ServiceException(
                    "Child reviews cannot have scores."
            );
        }

        if(!isChildReview && !hasScores){
            throw new CustomExceptions.ServiceException(
                    "Parent reviews must have scores."
            );
        }

        if(!hasScores){
            return;
        }

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
