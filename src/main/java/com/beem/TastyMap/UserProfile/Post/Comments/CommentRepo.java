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
            c.parentYorumId,
            c.contents,
            c.date,
            u.id,
            u.username,
            u.profile
        )
        from CommentEntity c
        join c.user u
        where c.postId = :postId
        and c.parentYorumId is null
        order by c.date desc
    """)
    Page<CommentsResponseDTO> getPostComments(
            @Param("postId") Long postId,
            Pageable pageable
    );

    @Query("""
        select new com.beem.TastyMap.UserProfile.Post.Comments.CommentsResponseDTO(
            c.id,
            c.parentYorumId,
            c.contents,
            c.date,
            u.id,
            u.username,
            u.profile
        )
        from CommentEntity c
        join c.user u
        where c.postId = :postId
        and c.parentYorumId = :parentCommentId
        order by c.date desc
    """)
    Page<CommentsResponseDTO> getCommentReplys(
            @Param("postId") Long postId,
            @Param("parentCommentId") Long parentCommentId,
            Pageable pageable
    );

}
