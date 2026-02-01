package com.beem.TastyMap.MapsReview.Entity;

import com.beem.TastyMap.MapsReview.Enum.ScoreType;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "review_score",
    uniqueConstraints = @UniqueConstraint(columnNames = {"review_id", "type"}))
public class ScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScoreType type;

    @Column(nullable = false)
    private double score;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private ReviewEntity review;

    public ScoreEntity() {
    }

    public ScoreEntity(ScoreType type, double score, ReviewEntity review) {
        this.type = type;
        this.score = score;
        this.review = review;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ScoreType getType() {
        return type;
    }

    public void setType(ScoreType type) {
        this.type = type;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public ReviewEntity getReview() {
        return review;
    }

    public void setReview(ReviewEntity review) {
        this.review = review;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScoreEntity)) return false;
        ScoreEntity that = (ScoreEntity) o;
        return this.type == that.type &&
                this.review != null &&
                this.review.getId().equals(that.review.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, review.getId());
    }

}
