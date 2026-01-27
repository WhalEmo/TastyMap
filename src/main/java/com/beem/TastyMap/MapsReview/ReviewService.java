package com.beem.TastyMap.MapsReview;

import com.beem.TastyMap.Maps.Entity.PlaceEntity;
import com.beem.TastyMap.Maps.Service.PlacesService;
import com.beem.TastyMap.MapsReview.Data.PlaceReviewRequest;
import com.beem.TastyMap.MapsReview.Data.ReviewResponse;
import com.beem.TastyMap.MapsReview.Data.ReviewResult;
import com.beem.TastyMap.MapsReview.Entity.ReviewEntity;
import com.beem.TastyMap.MapsReview.Entity.ScoreEntity;
import com.beem.TastyMap.MapsReview.Enum.ReviewSource;
import com.beem.TastyMap.MapsReview.Enum.ReviewStatus;
import com.beem.TastyMap.User.User;
import com.beem.TastyMap.User.UserService;
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
    private final UserService userService;
    private final EntityManager entityManager;

    public ReviewService(ReviewRepo reviewRepo,PlacesService placesService, UserService userService, EntityManager entityManager) {
        this.reviewRepo = reviewRepo;
        this.placesService = placesService;
        this.userService = userService;
        this.entityManager = entityManager;
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


    private ReviewEntity getParentReference(Long parentId) {
        if(parentId == null) {
            return null;
        }
        if(!reviewRepo.existsById(parentId)) {
            throw new RuntimeException("Parent review not found");
        }
        return entityManager.getReference(ReviewEntity.class, parentId);
    }

    @Transactional
    public void sendPlaceReview(PlaceReviewRequest request){
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

        User user = userService.getUserById(request.getUserId());


        ReviewEntity entity = new ReviewEntity(
                user.getUsername(),
                request.getContent(),
                ReviewSource.INTERNAL,
                place,
                user,
                this.getParentReference(
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

        reviewRepo.save(entity);
        reviewRepo.flush();
    }
}
