package com.beem.TastyMap.UserProfile.Post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepo extends JpaRepository<PostEntity,Long> {

    long countByUserId(Long userId);

    @Query("""
        select new com.beem.TastyMap.UserProfile.Post.PostResponseDTO(
            p.id,
            p.explanation,
            p.puan,
            p.photoUrl,
            p.createdAt,

            u.id,
            u.username,
            u.profilePhotoUrl,

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
        join UserEntity u on u.id = p.userId
        where p.userId = :userId
        order by p.createdAt desc
    """)
    Page<PostResponseDTO> getUserPosts(
            @Param("userId") Long userId,
            Pageable pageable
    );
}
