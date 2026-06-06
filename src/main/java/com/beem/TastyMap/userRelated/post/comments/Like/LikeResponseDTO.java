package com.beem.TastyMap.userRelated.post.comments.Like;

public class LikeResponseDTO {
    private boolean liked;
    private long totalLikes;

    public LikeResponseDTO(boolean liked, long totalLikes) {
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

