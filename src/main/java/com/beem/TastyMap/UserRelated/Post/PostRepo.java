package com.beem.TastyMap.UserRelated.Post;

import com.beem.TastyMap.UserRelated.Post.Comments.CommentRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostRepo extends JpaRepository<PostEntity,Long>,PostRepoCustom {

    long countByUserIdAndIsPinnedTrue(Long userId);

    @Query("SELECT COUNT(p) > 0 FROM PostEntity p WHERE p.id = :postId AND p.user.id = :userId")
    boolean isOwner(@Param("postId") Long postId, @Param("userId") Long userId);


    public interface PostStatsView {
        Long getOwnerId();
        int getNumberOfLikes();
    }
    @Query("SELECT p.user.id AS ownerId, p.numberofLikes AS numberOfLikes " +
            "FROM PostEntity p WHERE p.id = :postId")
    Optional<PostStatsView> findStatsByCPostId(@Param("postId") Long commentId);

     @Query("""
      SELECT p FROM PostEntity p
       JOIN FETCH p.user
       WHERE p.id = :postId
   """)
    Optional<PostEntity> findByIdWithUser(@Param("postId") Long postId);


    @Modifying
    @Query("""
    UPDATE PostEntity p
    SET p.commentCount = p.commentCount + 1
    WHERE p.id = :postId
            """)
    void incrementComment(@Param("postId")Long postId);

    @Modifying
    @Query("""
    UPDATE PostEntity p
    SET p.commentCount = p.commentCount - 1
    WHERE p.id = :postId
            """)
    void decrementComment(@Param("postId")Long postId);

    @Modifying
    @Query("""
   UPDATE PostEntity p
   SET p.numberofLikes = p.numberofLikes + 1
   WHERE p.id = :postId
""")
    void incrementLike(@Param("postId") Long postId);


    @Modifying
    @Query("""
   UPDATE PostEntity p
   SET p.numberofLikes = p.numberofLikes - 1
   WHERE p.id = :postId
     AND p.numberofLikes > 0
""")
    void decrementLike(@Param("postId") Long postId);


    @Query("""
    SELECT p.user.id as authorId, p.commentEnabled as commentEnabled 
    FROM PostEntity p 
    WHERE p.id = :id
""")
    Optional<PostStatusView> findPostStatusById(@Param("id") Long id);

    public interface PostStatusView {
        Long getAuthorId();
        boolean isCommentEnabled();
    }
}


