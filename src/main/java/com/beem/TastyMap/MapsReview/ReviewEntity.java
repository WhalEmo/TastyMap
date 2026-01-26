package com.beem.TastyMap.MapsReview;

import com.beem.TastyMap.Maps.Entity.PlaceEntity;
import com.beem.TastyMap.MapsReview.Enum.ReviewSource;
import com.beem.TastyMap.MapsReview.Enum.ReviewStatus;
import com.beem.TastyMap.User.User;
import jakarta.persistence.*;


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

    @PrePersist
    public void onCreate(){
        if(createdAt == null) createdAt = System.currentTimeMillis();
    }

    @PreUpdate
    public void onUpdate(){
        updateAt = System.currentTimeMillis();
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
}