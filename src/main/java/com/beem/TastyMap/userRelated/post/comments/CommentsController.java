package com.beem.TastyMap.userRelated.post.comments;

import com.beem.TastyMap.userRelated.post.comments.Like.LikeResponseDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
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
    @GetMapping("/getReplys/{postId}/{parentCommentId}")
    public Page<CommentsResponseDTO> getReplys(
            @PathVariable Long postId,
            @PathVariable Long parentCommentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        return commentService.getReplys(postId,myId,parentCommentId, page, size);
    }

    @PostMapping("/addComments/{postId}")
    public CommentsResponseDTO addComment(
            @Valid @RequestBody CommentRequestDTO dto,
            @PathVariable Long postId,
            Authentication authentication
    ){
        Long myId=(Long) authentication.getPrincipal();
       return commentService.addComment(myId,postId,dto);
    }

    @PostMapping("/addReply/{postId}/{parentCommentId}")
    public CommentsResponseDTO addReply(
            @Valid @RequestBody CommentRequestDTO dto,
            @PathVariable Long postId,
            @PathVariable Long parentCommentId,
            Authentication authentication
    ){
        Long myId=(Long) authentication.getPrincipal();
        return commentService.addReplyComment(myId,postId,parentCommentId,dto);
    }

    @DeleteMapping("/deleteComment/{postId}/{commentId}")
    public Map<String,String>deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            Authentication authentication
    ){
        Long myId=(Long) authentication.getPrincipal();
        commentService.deleteComment(postId,myId,commentId);

        return Map.of("message","Yorum silindi.");
    }

    @PutMapping("/updateComments/{postId}/{commentId}")
    public Map<String,String> updateComments(
            @Valid @RequestBody CommentRequestDTO dto,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            Authentication authentication
    ){
        Long myId=(Long) authentication.getPrincipal();
        commentService.updateComment(commentId,myId,dto);
        return Map.of("message","Yorum güncellendi.");
    }
    @PostMapping("/toggleLike/{commentId}")
    public LikeResponseDTO toggleLike(
            @PathVariable Long commentId,
            Authentication authentication
    ){
        Long myId=(Long) authentication.getPrincipal();

        return commentService.toggleLike(commentId,myId);
    }

    @PutMapping("/togglePin/{postId}/{commentId}")
    public CommentsResponseDTO togglePin(
            @PathVariable Long commentId,
            @PathVariable Long postId,
            Authentication authentication
    ){
        Long myId=(Long) authentication.getPrincipal();
        return commentService.togglePinComment(commentId,myId,postId);
    }

}
