package com.beem.TastyMap.UserProfile.Post.Comments;

import com.beem.TastyMap.UserProfile.Post.PostResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepo extends JpaRepository<CommentEntity,Long> {
    @Query("""
        select new com.beem.TastyMap.UserProfile.Post.Comments.CommentsResponseDTO(
            c.id,
            c.parentComment.id,
            c.post.id,
            c.contents,
            c.date,
            c.numberofLikes,
            u.id,
            u.username,
            u.profile
        )
        from CommentEntity c
        join c.user u
        where c.post.id = :postId
        and c.parentComment is null
        order by c.date desc
    """)
    Page<CommentsResponseDTO> getPostComments(
            @Param("postId") Long postId,
            Pageable pageable
    );

    @Query("""
        select new com.beem.TastyMap.UserProfile.Post.Comments.CommentsResponseDTO(
            c.id,
            c.parentComment.id,
            c.post.id,
            c.contents,
            c.date,
            c.numberofLikes,
            u.id,
            u.username,
            u.profile
        )
        from CommentEntity c
        join c.user u
        where c.post.id = :postId
        and c.parentComment.id = :parentCommentId
        order by c.date desc
    """)
    Page<CommentsResponseDTO> getCommentReplys(
            @Param("postId") Long postId,
            @Param("parentCommentId") Long parentCommentId,
            Pageable pageable
    );

}
