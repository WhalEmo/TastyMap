package com.beem.TastyMap.UserProfile.Post.Comments;

import com.beem.TastyMap.UserProfile.Post.PostRequestDTO;
import com.beem.TastyMap.UserProfile.Post.PostResponseDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/Comments")
public class CommentsController {
    private final CommentService commentService;

    public CommentsController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/getComments/{postId}")
    public Page<CommentsResponseDTO> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        return commentService.getComments(postId,myId, page, size);
    }

    @PostMapping("/addComments/{postId}")
    public Map<String,String>addComment(
            @Valid @RequestBody CommentRequestDTO dto,
            @PathVariable Long postId,
            Authentication authentication
    ){
        Long myId=(Long) authentication.getPrincipal();
        commentService.addComment(myId,postId,dto);

        return Map.of("message","Yorum eklendi.");
    }

    @PostMapping("/addReply/{postId}")
    public Map<String,String>addReply(
            @Valid @RequestBody CommentRequestDTO dto,
            @PathVariable Long postId,
            @PathVariable Long parentCommentId,
            Authentication authentication
    ){
        Long myId=(Long) authentication.getPrincipal();
        commentService.addReplyComment(myId,postId,parentCommentId,dto);

        return Map.of("message","Yanıt eklendi.");
    }
}
