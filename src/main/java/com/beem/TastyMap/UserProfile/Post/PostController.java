package com.beem.TastyMap.UserProfile.Post;


import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/addPost")
    public void addPost(
            @RequestBody PostRequestDTO dto,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        postService.addPost(dto, myId);
    }

    // 🔹 Başkasının profiline ait postlar
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

    // 🔹 Kendi postlarım
    @GetMapping("/getMePosts")
    public Page<PostResponseDTO> getMyPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        return postService.getPosts(myId, myId, page, size);
    }


}
