package com.beem.TastyMap.mapsReview;

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
import com.beem.TastyMap.mapsReview.entity.QReviewEntity;
import com.beem.TastyMap.mapsReview.entity.ReviewEntity;
import com.beem.TastyMap.mapsReview.entity.ScoreEntity;
import com.beem.TastyMap.mapsReview.enums.ReviewSource;
import com.beem.TastyMap.mapsReview.enums.ReviewStatus;
import com.beem.TastyMap.mapsReview.enums.ScoreType;
import com.beem.TastyMap.mapsReview.repository.ReviewQueryRepository;
import com.beem.TastyMap.mapsReview.repository.ReviewRepo;
import com.beem.TastyMap.redis.RedisKeyGenerator;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.registerLogin.UserRepo;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
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
    private final ApplicationEventPublisher eventPublisher;
    private final JPAQueryFactory queryFactory;
    private final ReviewQueryRepository reviewQueryRepository;

    public ReviewService(ReviewRepo reviewRepo, PlacesService placesService,
                         EntityManager entityManager, UserRepo userRepo, ReviewMapper reviewMapper,
                         ApplicationEventPublisher eventPublisher, JPAQueryFactory queryFactory, ReviewQueryRepository reviewQueryRepository) {
        this.reviewRepo = reviewRepo;
        this.placesService = placesService;
        this.entityManager = entityManager;
        this.userRepo = userRepo;
        this.reviewMapper = reviewMapper;
        this.eventPublisher = eventPublisher;
        this.queryFactory = queryFactory;
        this.reviewQueryRepository = reviewQueryRepository;
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
        List<ReviewResult> reviewResults = reviewQueryRepository
                .findReviews(placeId, page, size);

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
    public ReviewResult sendPlaceReview(SentReviewReq request, Long userId){
        if (request.getMainRating() == null || request.getMainRating() < 0.5 || request.getMainRating() > 5.0) {
            throw new CustomExceptions.ServiceException("Rating must be between 0.5 and 5.0");
        }

        if (request.getParentId() == null) {
            boolean alreadyReviewed = reviewRepo.existsByPlace_PlaceIdAndUserIdAndParentIsNullAndStatus(
                    request.getPlaceId(),
                    userId,
                    ReviewStatus.APPROVED
            );
            if (alreadyReviewed) {
                throw new CustomExceptions.InvalidException("Bu mekana zaten bir değerlendirme yaptınız. Mevcut yorumunuzu güncelleyebilirsiniz.");
            }
        }

        PlaceEntity place = placesService.getReferenceIfExists(request.getPlaceId());

        createReviewRequestDtoControl(request, place.getId());

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

        entity.setRating(request.getMainRating());

        List<ScoreDto> scoreDto = applyScoreListCreate(request, entity);

        reviewRepo.saveAndFlush(entity);

        if (request.getParentId() == null) {
            updateTastyMapPlaceStats(place.getId());
        }

        return new ReviewResult(
                entity.getId(),
                user.getId(),
                user.getUsername(),
                user.getProfile(),
                entity.getRating(),
                entity.getText(),
                entity.getSource(),
                entity.getParent() != null ? entity.getParent().getId() : null,
                entity.getLikeCount(),
                entity.getCreatedAt(),
                entity.getUpdateAt(),
                scoreDto
        );
    }


    @Transactional
    public Map<String, Object> deletePlaceReview(Long reviewId, Long userId){
        ReviewEntity review =
                getReviewByUserIdAndReviewId(userId, reviewId);
        if(review.getStatus() != ReviewStatus.APPROVED) throw new CustomExceptions.InvalidException("Review is not APPROVED.");
        review.setStatus(ReviewStatus.REJECTED);

        reviewRepo.rejectChildReviews(reviewId, ReviewStatus.REJECTED);
        reviewRepo.saveAndFlush(review);

        if (review.getPlace() != null && review.getParent() == null) {
            updateTastyMapPlaceStats(review.getPlace().getId());
        }

        return reviewMapper.toDeletedReviewRes();
    }

    @Transactional
    public ReviewResult patchPlaceReview(UpdateReviewReq request, Long userId){
        ReviewEntity review =
                getReviewByUserIdAndReviewId(userId, request.getReviewId());

        //Security
        if(review.getStatus() != ReviewStatus.APPROVED)
            throw new CustomExceptions.ServiceException("This review not APPROVED.");

        if (request.getMainRating() == null || request.getMainRating() < 0.5 || request.getMainRating() > 5.0) {
            throw new CustomExceptions.ServiceException("Rating must be between 0.5 and 5.0");
        }
        updateScoreRatingControl(request);
        updateChildParentScoreControl(review, request);


        review.setRating(request.getMainRating());
        review.setText(request.getContent() != null ? request.getContent().trim() : null);
        review.setUpdateAt(System.currentTimeMillis());

        //Apply
        applyScoreUpdates(review, request);
        applyScoreDelete(review, request);

        ReviewEntity updatedReview = reviewRepo.saveAndFlush(review);

        if (review.getPlace() != null) {
            updateTastyMapPlaceStats(review.getPlace().getId());
        }

        List<ScoreDto> currentScoreDtos = updatedReview.getScores() != null
                ? updatedReview.getScores().stream()
                .map(s -> new ScoreDto(s.getType(), s.getScore()))
                .toList()
                : List.of();

        UserEntity user = updatedReview.getUser();

        return new ReviewResult(
                updatedReview.getId(),
                user != null ? user.getId() : null,
                updatedReview.getAuthorName(),
                user != null ? user.getProfile() : null,
                updatedReview.getRating(),
                updatedReview.getText(),
                updatedReview.getSource(),
                updatedReview.getParent() != null ? updatedReview.getParent().getId() : null,
                updatedReview.getLikeCount(),
                updatedReview.getCreatedAt(),
                updatedReview.getUpdateAt(),
                currentScoreDtos
        );
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

        ScoreEntity overAll = existing.get(ScoreType.OVERALL);

        if (overAll != null) {
            overAll.setScore(request.getMainRating());
        } else {
            review.getScores().add(
                    new ScoreEntity(
                            ScoreType.OVERALL,
                            request.getMainRating(),
                            review
                    )
            );
        }
    }

    private void applyScoreDelete(ReviewEntity review, UpdateReviewReq request){
        if (request.getScores() == null) return;
        Set<ScoreType> incomingTypes =
                request.getScores().stream()
                        .map(ScoreDto::getType)
                        .collect(Collectors.toSet());

        review.getScores().removeIf(
                score -> score.getType() != ScoreType.OVERALL && !incomingTypes.contains(score.getType())
        );
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
    }

    private void updateTastyMapPlaceStats(Long placeId) {
        QReviewEntity review = QReviewEntity.reviewEntity;

        NumberExpression<Double> avgScoreExpr = review.rating.avg();
        NumberExpression<Long> countExpr = review.count();

        Tuple stats = queryFactory
                .select(avgScoreExpr, countExpr)
                .from(review)
                .where(
                        review.place.id.eq(placeId),
                        review.source.eq(ReviewSource.INTERNAL),
                        review.parent.isNull(),
                        review.status.eq(ReviewStatus.APPROVED),
                        review.deleted.isFalse()
                )
                .fetchOne();

        Double avgScore = stats != null ? stats.get(avgScoreExpr) : null;
        Long reviewCount = stats != null ? stats.get(countExpr) : 0L;

        double calculatedRating = (avgScore != null) ? Math.round(avgScore * 10.0) / 10.0 : 0.0;
        int calculatedCount = (reviewCount != null) ? reviewCount.intValue() : 0;

        PlaceEntity place = placesService.findById(placeId);
        place.setTastyMapRating(calculatedRating);
        place.setTastyMapReviewCount(calculatedCount);

        placesService.save(place);
        placesService.evictPlaceCache(place.getPlaceId());
    }

    private void createReviewRequestDtoControl(SentReviewReq request, Long placeId){
        if(request.getParentId() != null){
            if(
                    !reviewRepo.existsByIdAndPlaceIdAndSourceAndStatus(request.getParentId(),
                            placeId,
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

    private List<ScoreDto> applyScoreListCreate(SentReviewReq request, ReviewEntity entity){

        boolean isChildReview = request.getParentId() != null;
        boolean hasScores = request.getScores() != null && !request.getScores().isEmpty();

        if(isChildReview && hasScores){
            throw new CustomExceptions.ServiceException(
                    "Child reviews cannot have scores."
            );
        }


        List<ScoreEntity> scoreEntities = new ArrayList<>();

        if (!isChildReview && request.getMainRating() != null) {
            scoreEntities.add(new ScoreEntity(ScoreType.OVERALL, request.getMainRating(), entity));
        }

        if (hasScores) {
            for (ScoreDto scoreDto : request.getScores()) {
                if (scoreDto.getType() != ScoreType.OVERALL) {
                    scoreEntities.add(new ScoreEntity(scoreDto.getType(), scoreDto.getScore(), entity));
                }
            }
        }

        entity.setScores(scoreEntities);

        return scoreEntities.stream()
                .map(scoreEntity -> {
                    return new ScoreDto(
                            scoreEntity.getType(),
                            scoreEntity.getScore()
                    );
                }).toList();
    }

}
