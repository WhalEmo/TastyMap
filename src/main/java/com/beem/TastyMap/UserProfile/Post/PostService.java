package com.beem.TastyMap.UserProfile.Post;

import com.beem.TastyMap.Exceptions.CustomExceptions;
import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.RegisterLogin.UserRepo;
import com.beem.TastyMap.UserProfile.Block.BlockRepo;
import com.beem.TastyMap.UserProfile.Subscribe.SubscribeRepo;
import com.beem.TastyMap.UserProfile.Subscribe.SubscribeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {
    private final PostRepo postRepo;
    private final SubscribeRepo subscribeRepo;
    private final UserRepo userRepo;
    private final BlockRepo blockRepo;

    public PostService(PostRepo postRepo, SubscribeRepo subscribeRepo, UserRepo userRepo, BlockRepo blockRepo) {
        this.postRepo = postRepo;
        this.subscribeRepo = subscribeRepo;
        this.userRepo = userRepo;
        this.blockRepo = blockRepo;
    }

    @Transactional
    public void addPost(PostRequestDTO dto,Long myId){
        PlaceEmbedded place=new PlaceEmbedded();
        place.setPlaceId(dto.getPlaceId());
        place.setCity(dto.getCity());
        place.setCategories(dto.getCategories());
        place.setAveragePuan(dto.getAveragePuan());
        place.setDistrict(dto.getDistrict());
        place.setNeighbourhood(dto.getNeighbourhood());
        place.setPlaceName(dto.getPlaceName());
        place.setLatitude(dto.getLatitude());
        place.setLongitude(dto.getLongitude());

        PostEntity post=new PostEntity();
        post.setExplanation(dto.getExplanation());
        post.setPuan(dto.getPuan());
        post.setUserId(myId);
        post.setPhotoUrl(dto.getPhotoUrl());
        post.setPlaceEmbedded(place);
        postRepo.save(post);
    }

    //postları getirme post sayısı
    public Page<PostResponseDTO> getPosts(Long userId, Long myId, int page, int size) {
        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Kullanıcı bulunamadı"));

        boolean blocked =
                blockRepo.existsByBlockerIdAndBlockedId(userId, myId) ||
                        blockRepo.existsByBlockerIdAndBlockedId(myId, userId);

        if (blocked) {
            throw new CustomExceptions.ForbiddenException("Bu kullanıcının postlarını görüntüleyemezsiniz");
        }

        if (!userId.equals(myId) && user.isPrivateProfile()) {
            boolean isFollowing = subscribeRepo.existsBySubscriberIdAndSubscribedId(myId, userId);

            if (!isFollowing) {
                throw new CustomExceptions.ForbiddenException("Bu kullanıcının postlarını görmek için takip etmelisin");
            }
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepo.getUserPosts(userId, pageable);
    }

}










