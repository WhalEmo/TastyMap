package com.beem.TastyMap.userRelated.visit;

import com.beem.TastyMap.exceptions.CustomExceptions;
import com.beem.TastyMap.registerLogin.UserEntity;
import com.beem.TastyMap.userRelated.post.PlaceEmbedded;
import com.beem.TastyMap.userRelated.post.PostAndVisitRequestDTO;
import com.beem.TastyMap.userRelated.post.PostResponseDTO;
import com.beem.TastyMap.userRelated.post.PostService;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class VisitService {
    private final VisitRepo visitRepo;
    private final PostService postService;
    private final EntityManager entityManager;

    public VisitService(VisitRepo visitRepo, PostService postService, EntityManager entityManager) {
        this.visitRepo = visitRepo;
        this.postService = postService;
        this.entityManager = entityManager;
    }

    @Transactional
    public PostResponseDTO processVisitAction(PostAndVisitRequestDTO combinedDto, Long myId) {
        saveVisit(combinedDto, myId);
        if (combinedDto.isWantToPost()) {
            return postService.addPost(combinedDto, myId);
        }
        return null;
    }

    private void saveVisit(VisitRequestDTO dto, Long myId) {
        VisitEntity visit = new VisitEntity();
        visit.setUser(entityManager.getReference(UserEntity.class, myId));

        PlaceEmbedded place = new PlaceEmbedded();
        place.setPlaceId(dto.getPlaceId());
        place.setPlaceName(dto.getPlaceName());
        place.setCity(dto.getCity());
        place.setDistrict(dto.getDistrict());
        place.setNeighbourhood(dto.getNeighbourhood());
        place.setLatitude(dto.getLatitude());
        place.setLongitude(dto.getLongitude());
        place.setCategories(dto.getCategories());
        place.setAveragePuan(dto.getAveragePuan());

        visit.setPlaceEmbedded(place);
        visit.setCreatedAt(LocalDateTime.now());
        visitRepo.save(visit);
    }

    public Page<VisitResponseDTO> getVisits(Long userId, int page, int size){

        Pageable pageable = PageRequest.of(page, size);
        return visitRepo.findUserVisits(userId, pageable);
    }

    public void deleteVisit(Long visitId,Long userId){
        VisitEntity visit= visitRepo.findById(visitId)
                .orElseThrow(()->new CustomExceptions.NotFoundException("Ziyaret bulunamadı"));
        if(!visit.getUser().getId().equals(userId)){
            throw new CustomExceptions.AuthorizationException("Bu ziyareti silme yetkiniz yok.");
        }
        visit.setDelete(true);
        visitRepo.save(visit);
    }
}
