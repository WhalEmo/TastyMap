package com.beem.TastyMap.UserRelated.Post.Comments;

import com.beem.TastyMap.Exceptions.CustomExceptions;
import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.UserRelated.Post.AccessChecker;
import com.beem.TastyMap.UserRelated.Post.Comments.Like.LikeEntity;
import com.beem.TastyMap.UserRelated.Post.Comments.Like.LikeRepo;
import com.beem.TastyMap.UserRelated.Post.PostEntity;
import com.beem.TastyMap.UserRelated.Post.PostRepo;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CommentService {
    private final CommentRepo commentRepo;
    private final PostRepo postRepo;
    private final EntityManager entityManager;
    private final AccessChecker accessChecker;
    private final LikeRepo likeRepo;

    public CommentService(CommentRepo commentRepo, PostRepo postRepo, EntityManager entityManager, AccessChecker accessChecker, LikeRepo likeRepo) {
        this.commentRepo = commentRepo;
        this.postRepo = postRepo;
        this.entityManager = entityManager;
        this.accessChecker = accessChecker;
        this.likeRepo = likeRepo;
    }

    @Transactional
    public CommentsResponseDTO addComment(Long myId, Long postId, CommentRequestDTO dto) {
        PostEntity post = postRepo.findById(postId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Post bulunamadı"));

        accessChecker.checkAccess(post.getUser().getId(), myId);
        UserEntity commentRef = entityManager.getReference(UserEntity.class, myId);

        CommentEntity comment = new CommentEntity();
        comment.setPost(post);
        comment.setUser(commentRef);
        comment.setParentComment(null);
        comment.setContents(dto.getContents().trim());
        comment.setNumberofLikes(0);
        commentRepo.save(comment);
        return new CommentsResponseDTO(comment);
    }

    @Transactional
    public CommentsResponseDTO addReplyComment(Long myId,Long postId,Long parentCommentId,CommentRequestDTO dto){
        PostEntity post = postRepo.findById(postId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Post bulunamadı"));

        accessChecker.checkAccess(post.getUser().getId(), myId);
        UserEntity commentRef = entityManager.getReference(UserEntity.class, myId);

        CommentEntity parentComment = commentRepo.findById(parentCommentId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Yanıt verilecek yorum bulunamadı"));

        if (!parentComment.getPost().getId().equals(postId)) {
            throw new CustomExceptions.ForbiddenException("Yanıt verilen yorum bu posta ait değil");
        }

        CommentEntity comment=new CommentEntity();
        comment.setPost(post);
        comment.setUser(commentRef);
        comment.setParentComment(parentComment);
        comment.setContents(dto.getContents().trim());
        comment.setNumberofLikes(0);
        commentRepo.save(comment);
        return new CommentsResponseDTO(comment);
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
        return commentRepo.getCommentReplys(postId,parentCommentId,pageable);
    }


    @Transactional
    public void deleteComment(Long postId,Long myId,Long commentId){
        CommentEntity comment = commentRepo.findById(commentId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Yorum bulunamadı"));

        if (!comment.getPost().getId().equals(postId)) {
            throw new CustomExceptions.ForbiddenException("Yorum bu posta ait değil");
        }
        boolean isPostOwner = postRepo.existsByIdAndUser_Id(postId, myId);
        boolean isCommentOwner = comment.getUser().getId().equals(myId);

        if (!isPostOwner && !isCommentOwner) {
            throw new CustomExceptions.ForbiddenException("Bu yorumu silme yetkiniz yok");
        }
        commentRepo.delete(comment);
    }


    @Transactional
    public void updateComment(Long commentId,Long userId,Long postId,CommentRequestDTO dto){
        CommentEntity comment=commentRepo.findById(commentId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Yorum bulunamadı."));

        if (!comment.getPost().getId().equals(postId)) {
            throw new CustomExceptions.ForbiddenException("Yorum bu posta ait değil");
        }
        if(!comment.getUser().getId().equals(userId)){
            throw new CustomExceptions.NotFoundException("Bu yorumu güncelleme yetkiniz yok.");
        }
        comment.setContents(dto.getContents());
        commentRepo.save(comment);
    }

    @Transactional
    public String toggleLike(Long commentId,Long userId){
        CommentEntity comment=commentRepo.findById(commentId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Yorum bulunamadı."));

        accessChecker.checkAccess(comment.getPost().getUser().getId(), userId);
        Optional<LikeEntity> existingLike = likeRepo.findByComment_IdAndUser_Id(commentId, userId);
        if(existingLike.isPresent()){
            likeRepo.delete(existingLike.get());
            commentRepo.decrementLike(commentId);
            return "Beğeni kaldırıldı.";
        }else{
            UserEntity userRef = entityManager.getReference(UserEntity.class, userId);
            LikeEntity like=new LikeEntity();
            like.setComment(comment);
            like.setUser(userRef);
            likeRepo.saveAndFlush(like);
            commentRepo.incrementLike(commentId);
            return "Beğenildi.";
        }
    }
}
