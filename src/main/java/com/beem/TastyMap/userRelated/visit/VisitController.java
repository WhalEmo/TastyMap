package com.beem.TastyMap.userRelated.visit;


import com.beem.TastyMap.userRelated.post.PostAndVisitRequestDTO;
import com.beem.TastyMap.userRelated.post.PostResponseDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/visits")
public class VisitController {
    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }
    @PostMapping("/visit_and_post")
    public ResponseEntity<?> addVisitAndPost(
            @Valid @RequestBody PostAndVisitRequestDTO dto,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        PostResponseDTO postResponse = visitService.processVisitAction(dto, myId);

        if (postResponse != null) {
            return ResponseEntity.ok(postResponse);
        }
        return ResponseEntity.ok("Ziyaret başarıyla kaydedildi.");
    }

    @GetMapping("/getVisit")
    public Page<VisitResponseDTO> getMyVisits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication authentication
    ){
        Long myId = (Long) authentication.getPrincipal();
        return visitService.getVisits(myId,page,size);
    }

    @PatchMapping("/deleteVisit/{visitId}")
    public Map<String,String> deleteVisit(
            @PathVariable Long visitId,
            Authentication authentication
    ){
        Long myId = (Long) authentication.getPrincipal();
         visitService.deleteVisit(visitId,myId);
        return Map.of("meesage","Ziyaret silindi");
    }
}
