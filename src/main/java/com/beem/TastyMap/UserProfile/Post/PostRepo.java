package com.beem.TastyMap.UserProfile.Post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepo extends JpaRepository<PostEntity,Long> {

    long countByUser_Id(Long userId);
    boolean existsByIdAndUser_Id(Long postId, Long myId);

    @Query("""
        select new com.beem.TastyMap.UserProfile.Post.PostResponseDTO(
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
}
