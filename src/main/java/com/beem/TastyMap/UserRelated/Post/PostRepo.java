package com.beem.TastyMap.UserRelated.Post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepo extends JpaRepository<PostEntity,Long> {

    long countByUser_Id(Long userId);
    boolean existsByIdAndUser_Id(Long postId, Long myId);

    @Query("""
        select new com.beem.TastyMap.UserRelated.Post.PostResponseDTO(
            p.id,
            p.explanation,
            p.puan,
            p.photoUrl,
            p.numberofLikes,
            p.createdAt,

            u.id,
            u.username,
            u.profile,

            p.placeEmbedded.placeId,
            p.placeEmbedded.placeName,
            p.placeEmbedded.categories,
            p.placeEmbedded.city,
            p.placeEmbedded.district,
            p.placeEmbedded.neighbourhood,
            p.placeEmbedded.latitude,
            p.placeEmbedded.longitude,
            p.placeEmbedded.averagePuan
        )
            from PostEntity p
                join p.user u
                where u.id = :userId
        
    """)
    Page<PostResponseDTO> getUserPosts(
            @Param("userId") Long userId,
            Pageable pageable
    );

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

}
