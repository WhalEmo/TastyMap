package com.beem.TastyMap.UserRelated.Post.Comments;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface CommentRepoCustom {
    Page<CommentsResponseDTO> getPostComments(@Param("postId") Long postId,Long myId, Pageable pageable);
    Page<CommentsResponseDTO> getCommentReplys(@Param("postId") Long postId,Long myId, @Param("parentCommentId") Long parentCommentId, Pageable pageable);
}
