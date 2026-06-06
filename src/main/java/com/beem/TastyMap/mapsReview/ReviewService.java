package com.beem.TastyMap.mapsReview;

import com.beem.TastyMap.maps.entity.PlaceEntity;
import com.beem.TastyMap.maps.service.PlacesService;
import com.beem.TastyMap.mapsReview.data.Request.SentReviewReq;
import com.beem.TastyMap.mapsReview.data.Request.UpdateReviewReq;
import com.beem.TastyMap.mapsReview.data.Response.CreatedReviewRes;
import com.beem.TastyMap.mapsReview.data.Response.ReviewResponse;
import com.beem.TastyMap.mapsReview.data.ReviewResult;
import com.beem.TastyMap.mapsReview.entity.ReviewEntity;
import com.beem.TastyMap.mapsReview.entity.ScoreEntity;
import com.beem.TastyMap.mapsReview.Enum.ReviewSource;
import com.beem.TastyMap.mapsReview.Enum.ReviewStatus;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.registerLogin.UserService;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


@Service
public class ReviewService {

    private final ReviewRepo reviewRepo;
    private final PlacesService placesService;
    private final EntityManager entityManager;
    private final UserService userService;

    public ReviewService(ReviewRepo reviewRepo, PlacesService placesService, EntityManager entityManager, UserService userService) {
        this.reviewRepo = reviewRepo;
        this.placesService = placesService;
        this.entityManager = entityManager;
        this.userService = userService;
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

    @Transactional
    public CreatedReviewRes sendPlaceReview(SentReviewReq request){
        if (request.getParentId() != null &&
                !reviewRepo.existsByIdAndPlaceIdAndSourceAndStatus(
                        request.getParentId(),
                        request.getPlaceId(),
                        ReviewSource.INTERNAL,
                        ReviewStatus.APPROVED
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Parent review must be an approved internal review of this place"
            );
        }

        PlaceEntity place = placesService.getReferenceIfExists(request.getPlaceId());

        UserEntity user = new UserEntity();


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

        reviewRepo.saveAndFlush(entity);

        return new CreatedReviewRes(
                entity.getId(),
                place.getId(),
                entity.getAuthorName(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    @Transactional
    public CreatedReviewRes updatePlaceReview(UpdateReviewReq request){
        ReviewEntity review = getReviewEntity(request.getReviewId());

        review.setScores(
                request.getScores()
                        .stream()
                        .map(scoreRequest -> {
                            return new ScoreEntity(
                                    scoreRequest.getType(),
                                    scoreRequest.getScore(),
                                    review
                            );
                        })
                        .toList()
        );

        if(request.getContent() != null || !request.getContent().isEmpty()) review.setText(request.getContent());

        return null;
    }
}
