package com.beem.TastyMap.userRelated.post.like;

public class PostLikeDTO {
    private boolean liked;
    private long totalLikes;

    public PostLikeDTO(boolean liked, long totalLikes) {
        this.liked = liked;
        this.totalLikes = totalLikes;
    }

    public boolean isLiked() {
        return liked;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }

    public long getTotalLikes() {
        return totalLikes;
    }

    public void setTotalLikes(long totalLikes) {
        this.totalLikes = totalLikes;
    }
}
