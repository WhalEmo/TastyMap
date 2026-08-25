package com.beem.TastyMap.mapsReview.repository;

import com.beem.TastyMap.mapsReview.data.ReviewResult;
import com.beem.TastyMap.mapsReview.data.ScoreDto;
import com.beem.TastyMap.mapsReview.enums.ReviewSource;

import java.util.ArrayList;
import java.util.List;

public class ReviewResultBuilder {
    private final Long id;
    private final Long userId;
    private final String name;
    private final String userProfile;
    private final double rating;
    private final String content;
    private final ReviewSource source;
    private final Long parentId;
    private final Integer likeCount;
    private final Long createdAt;
    private final Long updateAt;
    private final List<ScoreDto> scores = new ArrayList<>();

    public ReviewResultBuilder(Long id, Long userId, String name, String userProfile, double rating,
                               String content, ReviewSource source, Long parentId,
                               Integer likeCount, Long createdAt, Long updateAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.userProfile = userProfile;
        this.rating = rating;
        this.content = content;
        this.source = source;
        this.parentId = parentId;
        this.likeCount = likeCount;
        this.createdAt = createdAt;
        this.updateAt = updateAt;
    }

    public void addScore(ScoreDto score) {
        if (score != null) {
            this.scores.add(score);
        }
    }

    public ReviewResult build() {
        return new ReviewResult(
                id,
                userId,
                name,
                userProfile,
                rating,
                content,
                source,
                parentId,
                likeCount,
                createdAt,
                updateAt,
                scores
        );
    }
}