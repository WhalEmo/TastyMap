package com.beem.TastyMap.MapsReview;

import com.beem.TastyMap.Maps.Entity.PlaceEntity;
import com.beem.TastyMap.Maps.Service.PlacesService;
import com.beem.TastyMap.MapsReview.Data.Request.SentReviewReq;
import com.beem.TastyMap.MapsReview.Data.Request.UpdateReviewReq;
import com.beem.TastyMap.MapsReview.Data.Response.CreatedReviewRes;
import com.beem.TastyMap.MapsReview.Data.Response.ReviewResponse;
import com.beem.TastyMap.MapsReview.Data.ReviewResult;
import com.beem.TastyMap.MapsReview.Entity.ReviewEntity;
import com.beem.TastyMap.MapsReview.Entity.ScoreEntity;
import com.beem.TastyMap.MapsReview.Enum.ReviewSource;
import com.beem.TastyMap.MapsReview.Enum.ReviewStatus;
import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.RegisterLogin.UserService;
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
