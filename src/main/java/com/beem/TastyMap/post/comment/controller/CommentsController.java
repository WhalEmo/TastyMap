package com.beem.TastyMap.post.comment.controller;

import com.beem.TastyMap.post.comment.dto.CommentRequestDTO;
import com.beem.TastyMap.post.comment.service.CommentService;
import com.beem.TastyMap.post.comment.dto.CommentsResponseDTO;
import com.beem.TastyMap.post.comment.like.LikeResponseDTO;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentsController {
    private final CommentService commentService;
    private final MessageSource messageSource;

    public CommentsController(CommentService commentService, MessageSource messageSource) {
        this.commentService = commentService;
        this.messageSource = messageSource;
    }

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    @GetMapping("/get-comments/{postId}")
    public Page<CommentsResponseDTO> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        return commentService.getComments(postId, myId, page, size);
    }

    @GetMapping("/get-replys/{postId}/{parentCommentId}")
    public Page<CommentsResponseDTO> getReplys(
            @PathVariable Long postId,
            @PathVariable Long parentCommentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        return commentService.getReplys(postId, myId, parentCommentId, page, size);
    }

    @PostMapping("/add-comments/{postId}")
    public CommentsResponseDTO addComment(
            @Valid @RequestBody CommentRequestDTO dto,
            @PathVariable Long postId,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        return commentService.addComment(myId, postId, dto);
    }

    @PostMapping("/add-reply/{postId}/{parentCommentId}")
    public CommentsResponseDTO addReply(
            @Valid @RequestBody CommentRequestDTO dto,
            @PathVariable Long postId,
            @PathVariable Long parentCommentId,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        return commentService.addReplyComment(myId, postId, parentCommentId, dto);
    }

    @DeleteMapping("/delete-comment/{postId}/{commentId}")
    public Map<String, String> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        commentService.deleteComment(postId, myId, commentId);

        return Map.of("message", getMessage("comment.deleted.success"));
    }

    @PutMapping("/update-comments/{commentId}")
    public CommentsResponseDTO updateComments(
            @Valid @RequestBody CommentRequestDTO dto,
            @PathVariable Long commentId,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        return commentService.updateComment(commentId, myId, dto);
    }

    @PostMapping("/toggle-like/{commentId}")
    public LikeResponseDTO toggleLike(
            @PathVariable Long commentId,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();

        return commentService.toggleLike(commentId, myId);
    }

    @PutMapping("/toggle-pin/{postId}/{commentId}")
    public CommentsResponseDTO togglePin(
            @PathVariable Long commentId,
            @PathVariable Long postId,
            Authentication authentication
    ) {
        Long myId = (Long) authentication.getPrincipal();
        return commentService.togglePinComment(commentId, myId, postId);
    }
}