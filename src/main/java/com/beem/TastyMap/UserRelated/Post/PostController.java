package com.beem.TastyMap.UserRelated.Post;


import com.beem.TastyMap.UserRelated.Post.Like.PostLikeDTO;
import com.beem.TastyMap.UserRelated.Post.Like.PostLikeUserDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/addPost")
    public void addPost(
            @Valid @RequestBody PostRequestDTO dto,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        postService.addPost(dto, myId);
    }

    @GetMapping("/getUserPosts/{userId}")
    public Page<PostResponseDTO> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        return postService.getPosts(userId, myId, page, size);
    }

    @GetMapping("/getMePosts")
    public Page<PostResponseDTO> getMyPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        return postService.getPosts(myId, myId, page, size);
    }

    @DeleteMapping("/deletePost/{postId}")
    public Map<String,String> deletePost(
            @PathVariable Long postId,
            Authentication authentication
    ){
        Long myId = (Long) authentication.getPrincipal();
        postService.deletePost(postId,myId);
        return Map.of("meesage","Post silindi");
    }

    @PutMapping("/updatePost/{postId}")
    public Map<String,String> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateDTO dto,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        postService.updatePost(postId, myId, dto);
        return Map.of("message","Post güncellendi");
    }

    @PostMapping("/toggleLike/{postId}")
    public PostLikeDTO toggleLike(
            @PathVariable Long postId,
            Authentication authentication
    ){
        Long myId=(Long) authentication.getPrincipal();
        return postService.toggleLike(postId,myId);
    }

    @GetMapping("/whosLike/{postId}")
    public Page<PostLikeUserDTO> whosLike(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        return postService.whosLike(postId, myId, page, size);
    }

    @PutMapping("/togglePin/{postId}")
    public PostResponseDTO togglePin(
            @PathVariable Long postId,
            Authentication authentication
    ){
        Long myId=(Long) authentication.getPrincipal();
        return postService.togglePinPost(postId,myId);
    }


}
