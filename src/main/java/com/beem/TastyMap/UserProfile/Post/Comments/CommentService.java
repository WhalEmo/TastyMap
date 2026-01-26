package com.beem.TastyMap.UserProfile.Post.Comments;

import com.beem.TastyMap.Exceptions.CustomExceptions;
import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.UserProfile.Block.BlockRepo;
import com.beem.TastyMap.UserProfile.Post.AccessChecker;
import com.beem.TastyMap.UserProfile.Post.PostEntity;
import com.beem.TastyMap.UserProfile.Post.PostRepo;
import com.beem.TastyMap.UserProfile.Subscribe.SubscribeRepo;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {
    private final CommentRepo commentRepo;
    private final PostRepo postRepo;
    private final BlockRepo blockRepo;
    private final SubscribeRepo subscribeRepo;
    private final EntityManager entityManager;
    private final AccessChecker accessChecker;

    public CommentService(CommentRepo commentRepo, PostRepo postRepo, BlockRepo blockRepo, SubscribeRepo subscribeRepo, EntityManager entityManager, AccessChecker accessChecker) {
        this.commentRepo = commentRepo;
        this.postRepo = postRepo;
        this.blockRepo = blockRepo;
        this.subscribeRepo = subscribeRepo;
        this.entityManager = entityManager;
        this.accessChecker = accessChecker;
    }

    public void addComment(Long myId, Long postId, CommentRequestDTO dto) {
        PostEntity post = postRepo.findById(postId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Post bulunamadı"));

        accessChecker.checkAccess(post.getUser().getId(), myId);
        UserEntity commentRef = entityManager.getReference(UserEntity.class, myId);

        CommentEntity comment = new CommentEntity();
        comment.setPostId(postId);
        comment.setUser(commentRef);
        comment.setParentYorumId(null);
        comment.setContents(dto.getContents().trim());

        commentRepo.save(comment);
    }

    public void addReplyComment(Long myId,Long postId,Long parentCommentId,CommentRequestDTO dto){
        PostEntity post = postRepo.findById(postId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Post bulunamadı"));

        accessChecker.checkAccess(post.getUser().getId(), myId);
        UserEntity commentRef = entityManager.getReference(UserEntity.class, myId);

        if (!commentRepo.existsById(parentCommentId)) {
            throw new CustomExceptions.NotFoundException("Yanıt verilecek yorum bulunamadı");
        }
        CommentEntity comment=new CommentEntity();
        comment.setUser(commentRef);
        comment.setPostId(postId);
        comment.setContents(dto.getContents().trim());
        comment.setParentYorumId(parentCommentId);

        commentRepo.save(comment);
    }

    public Page<CommentsResponseDTO> getComments(Long postId,Long myId, int page, int size){
        PostEntity post=postRepo.findById(postId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Post bulunamadı"));
        accessChecker.checkAccess(post.getUser().getId(), myId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date"));
        return commentRepo.getPostComments(postId,pageable);
    }

    public Page<CommentsResponseDTO> getReplys(Long postId,Long myId,Long parentCommentId, int page, int size){
        PostEntity post=postRepo.findById(postId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Post bulunamadı"));
        accessChecker.checkAccess(post.getUser().getId(), myId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date"));

    }



}
