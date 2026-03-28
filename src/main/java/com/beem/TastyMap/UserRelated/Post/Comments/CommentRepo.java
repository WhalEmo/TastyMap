package com.beem.TastyMap.UserRelated.Post.Comments;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommentRepo extends JpaRepository<CommentEntity,Long>,CommentRepoCustom {

    boolean existsByIdAndPost_Id(Long id, Long postId);

    @Query("SELECT c.user.id FROM CommentEntity c WHERE c.id = :commentId")
    Optional<Long> findOwnerIdByCommentId(@Param("commentId") Long postId);

    @Modifying
    @Query("""
   UPDATE CommentEntity c
   SET c.numberofLikes = c.numberofLikes + 1
   WHERE c.id = :commentId
""")
    void incrementLike(@Param("commentId") Long commentId);


    @Modifying
    @Query("""
   UPDATE CommentEntity c
   SET c.numberofLikes = c.numberofLikes - 1
   WHERE c.id = :commentId
     AND c.numberofLikes > 0
""")
    void decrementLike(@Param("commentId") Long commentId);


    @Query("SELECT c.user.id as authorId, c.post.id as postId FROM CommentEntity c WHERE c.id = :id")
    Optional<CommentDeleteView> findDeleteViewByCommentId(@Param("id") Long id);

    public interface CommentDeleteView {
        Long getAuthorId();
        Long getPostId();
    }

}
