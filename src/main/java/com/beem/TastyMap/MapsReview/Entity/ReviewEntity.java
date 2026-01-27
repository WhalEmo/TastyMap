package com.beem.TastyMap.MapsReview.Entity;

import com.beem.TastyMap.Maps.Entity.PlaceEntity;
import com.beem.TastyMap.MapsReview.Enum.ReviewSource;
import com.beem.TastyMap.MapsReview.Enum.ReviewStatus;
import com.beem.TastyMap.User.User;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "place_reviews")
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String authorName;
    private Double rating;

    @Column(columnDefinition = "TEXT")
    private String text;

    private Long createdAt;
    private Long updateAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewSource source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private PlaceEntity place;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ReviewEntity parent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status;

    private Boolean deleted = false;

    private Integer likeCount = 0;

    @OneToMany(
            mappedBy = "review",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ScoreEntity> scores = new ArrayList<>();

    @PrePersist
    public void onCreate(){
        if(createdAt == null) createdAt = System.currentTimeMillis();
        if(source == null) source = ReviewSource.GOOGLE;
        if(source == ReviewSource.GOOGLE) createdAt *= 1000;
        if(status == null) status = ReviewStatus.APPROVED;
        this.rating = calculateTotalRating();
    }

    @PreUpdate
    public void onUpdate(){
        updateAt = System.currentTimeMillis();
        this.rating = calculateTotalRating();
    }

    private double calculateTotalRating(){
        if(scores == null || scores.isEmpty()) return 0;
        return scores
                .stream()
                .mapToDouble(ScoreEntity::getScore)
                .average()
                .orElse(0);
    }


    public ReviewEntity() {
    }

    public ReviewEntity(String authorName, Double rating, String text, Long createdAt, PlaceEntity place) {
        this.authorName = authorName;
        this.rating = rating;
        this.text = text;
        this.createdAt = createdAt;
        this.place = place;
    }

    public ReviewEntity(String authorName, String text, ReviewSource source,
                        PlaceEntity place, User user, ReviewEntity parent, ReviewStatus status
    ) {
        this.authorName = authorName;
        this.text = text;
        this.source = source;
        this.place = place;
        this.user = user;
        this.parent = parent;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public PlaceEntity getPlace() {
        return place;
    }

    public void setPlace(PlaceEntity place) {
        this.place = place;
    }


    public Long getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(Long updateAt) {
        this.updateAt = updateAt;
    }

    public ReviewSource getSource() {
        return source;
    }

    public void setSource(ReviewSource source) {
        this.source = source;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ReviewEntity getParent() {
        return parent;
    }

    public void setParent(ReviewEntity parent) {
        this.parent = parent;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public List<ScoreEntity> getScores() {
        return scores;
    }

    public void setScores(List<ScoreEntity> scores) {
        this.scores = scores;
    }
}