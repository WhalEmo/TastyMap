package com.beem.TastyMap.post.controller;

import com.beem.TastyMap.post.dto.PostAndVisitRequestDTO;
import com.beem.TastyMap.post.dto.PostGridResponseDTO;
import com.beem.TastyMap.post.dto.PostResponseDTO;
import com.beem.TastyMap.post.dto.PostUpdateDTO;
import com.beem.TastyMap.post.like.PostLikeDTO;
import com.beem.TastyMap.post.like.PostLikeUserDTO;
import com.beem.TastyMap.post.service.PostService;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;
    private final MessageSource messageSource;

    public PostController(PostService postService, MessageSource messageSource) {
        this.postService = postService;
        this.messageSource = messageSource;
    }

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    @PostMapping("/add")
    public ResponseEntity<PostResponseDTO> addPost(
            @Valid @RequestBody PostAndVisitRequestDTO dto,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        PostResponseDTO response = postService.addPost(dto, myId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping("/get-user-posts/{userId}")
    public Page<PostGridResponseDTO> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        return postService.getUserGridPosts(userId, myId, page, size);
    }


    @GetMapping("/get-me-posts")
    public Page<PostGridResponseDTO> getMyPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        return postService.getUserGridPosts(myId, myId, page, size);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponseDTO> getPostDetail(
            @PathVariable Long postId,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        PostResponseDTO postDetail = postService.getPostDetail(postId, myId);
        return ResponseEntity.ok(postDetail);
    }

    @DeleteMapping("/delete-post/{postId}")
    public Map<String, String> deletePost(
            @PathVariable Long postId,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        postService.deletePost(postId, myId);
        return Map.of("message", getMessage("post.deleted.success"));
    }

    @PatchMapping("/update-post/{postId}")
    public PostResponseDTO updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateDTO dto,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        postService.updatePost(postId, myId, dto);
        return  postService.updatePost(postId, myId, dto);
    }

    @PostMapping("/toggle-like/{postId}")
    public PostLikeDTO toggleLike(
            @PathVariable Long postId,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        return postService.toggleLike(postId, myId);
    }

    @GetMapping("/whos-like/{postId}")
    public Page<PostLikeUserDTO> whosLike(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        return postService.whosLike(postId, myId, page, size);
    }

    @PutMapping("/toggle-pin/{postId}")
    public PostResponseDTO togglePin(
            @PathVariable Long postId,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        return postService.togglePinPost(postId, myId);
    }
}