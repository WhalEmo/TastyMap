package com.beem.TastyMap.UserRelated.Post.Comments;

import com.beem.TastyMap.Exceptions.CustomExceptions;
import com.beem.TastyMap.RegisterLogin.UserEntity;
import com.beem.TastyMap.RegisterLogin.UserRepo;
import com.beem.TastyMap.UserRelated.Post.AccessChecker;
import com.beem.TastyMap.UserRelated.Post.Comments.Like.LikeEntity;
import com.beem.TastyMap.UserRelated.Post.Comments.Like.LikeRepo;
import com.beem.TastyMap.UserRelated.Post.Comments.Like.LikeResponseDTO;
import com.beem.TastyMap.UserRelated.Post.PostEntity;
import com.beem.TastyMap.UserRelated.Post.PostRepo;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CommentService {
    private final CommentRepo commentRepo;
    private final PostRepo postRepo;
    private final UserRepo userRepo;
    private final EntityManager entityManager;
    private final AccessChecker accessChecker;
    private final LikeRepo likeRepo;

    public CommentService(CommentRepo commentRepo, PostRepo postRepo, UserRepo userRepo, EntityManager entityManager, AccessChecker accessChecker, LikeRepo likeRepo) {
        this.commentRepo = commentRepo;
        this.postRepo = postRepo;
        this.userRepo = userRepo;
        this.entityManager = entityManager;
        this.accessChecker = accessChecker;
        this.likeRepo = likeRepo;
    }

    @Transactional
    public CommentsResponseDTO addComment(Long myId, Long postId, CommentRequestDTO dto) {
        var userView = userRepo.findUserProjectionById(myId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Kullanıcı bulunamadı."));

        PostRepo.PostStatusView postStatus = postRepo.findPostStatusById(postId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Post bulunamadı"));

        accessChecker.checkAccess(postStatus.getAuthorId(), myId);

        if(!postStatus.isCommentEnabled()){
            throw new CustomExceptions.ForbiddenException("Yorumlara kapalı");
        }
        UserEntity commentRef = entityManager.getReference(UserEntity.class, myId);
        PostEntity postRef = entityManager.getReference(PostEntity.class, postId);

        CommentEntity comment = new CommentEntity();
        comment.setPost(postRef);
        comment.setUser(commentRef);
        comment.setParentComment(null);
        comment.setContents(dto.getContents().trim());
        commentRepo.save(comment);
        postRepo.incrementComment(postId);

        CommentsResponseDTO response = new CommentsResponseDTO();
        response.setId(comment.getId());
        response.setPost_id(postId);
        response.setContents(comment.getContents());
        response.setNumberOfLikes(0);
        response.setDate(comment.getDate());
        response.setLiked(false);
        response.setPinned(false);
        response.setParentCommentId(null);

        response.setUserId(userView.getId());
        response.setUsername(userView.getUsername());
        response.setProfilePhotoUrl(userView.getProfile());

        return response;
    }

    @Transactional
    public CommentsResponseDTO addReplyComment(Long myId,Long postId,Long parentCommentId,CommentRequestDTO dto){
        PostRepo.PostStatusView postStatus = postRepo.findPostStatusById(postId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Post bulunamadı"));

        accessChecker.checkAccess(postStatus.getAuthorId(), myId);

        if(!postStatus.isCommentEnabled()){
            throw new CustomExceptions.ForbiddenException("Yorumlara kapalı");
        }

        UserEntity commentRef = entityManager.getReference(UserEntity.class, myId);
        PostEntity postRef = entityManager.getReference(PostEntity.class, postId);
        CommentEntity parentRef = entityManager.getReference(CommentEntity.class, parentCommentId);

        boolean parentExistsInPost = commentRepo.existsByIdAndPost_Id(parentCommentId, postId);
        if (!parentExistsInPost) {
            throw new CustomExceptions.NotFoundException("Yanıt verilecek yorum bu postta bulunamadı");
        }
        var userView = userRepo.findUserProjectionById(myId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Kullanıcı bulunamadı"));

        CommentEntity reply=new CommentEntity();
        reply.setPost(postRef);
        reply.setUser(commentRef);
        reply.setParentComment(parentRef);
        reply.setContents(dto.getContents().trim());
        reply.setNumberofLikes(0);
        commentRepo.save(reply);

        CommentsResponseDTO response = new CommentsResponseDTO();
        response.setId(reply.getId());
        response.setPost_id(postId);
        response.setContents(reply.getContents());
        response.setNumberOfLikes(0);
        response.setDate(reply.getDate());
        response.setLiked(false);
        response.setPinned(false);
        response.setParentCommentId(parentCommentId);

        response.setUserId(userView.getId());
        response.setUsername(userView.getUsername());
        response.setProfilePhotoUrl(userView.getProfile());

        return response;
    }

    public Page<CommentsResponseDTO>getComments(Long postId,Long myId, int page, int size){
        PostRepo.PostStatusView postStatus = postRepo.findPostStatusById(postId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Post bulunamadı"));

        accessChecker.checkAccess(postStatus.getAuthorId(), myId);

        if(!postStatus.isCommentEnabled()){
            throw new CustomExceptions.ForbiddenException("Yorumlara kapalı");
        }
        Pageable pageable = PageRequest.of(page, size);
        return commentRepo.getPostComments(postId,myId,pageable);
    }

    public Page<CommentsResponseDTO> getReplys(Long postId,Long myId,Long parentCommentId, int page, int size){
        PostRepo.PostStatusView postStatus = postRepo.findPostStatusById(postId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Post bulunamadı"));
        accessChecker.checkAccess(postStatus.getAuthorId(), myId);

        if(!postStatus.isCommentEnabled()){
            throw new CustomExceptions.ForbiddenException("Yorumlara kapalı");
        }
        Pageable pageable = PageRequest.of(page, size);
        return commentRepo.getCommentReplys(postId,myId,parentCommentId,pageable);
    }


    @Transactional
    public void deleteComment(Long postId, Long myId, Long commentId) {
        CommentRepo.CommentDeleteView commentView = commentRepo.findDeleteViewByCommentId(commentId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Yorum bulunamadı"));

        if (!commentView.getPostId().equals(postId)) {
            throw new CustomExceptions.ForbiddenException("Yorum bu posta ait değil");
        }

        boolean isCommentOwner = commentView.getAuthorId().equals(myId);
        boolean isPostOwner = postRepo.isOwner(postId, myId);

        if (!isCommentOwner && !isPostOwner) {
            throw new CustomExceptions.ForbiddenException("Bu yorumu silme yetkiniz yok");
        }
        commentRepo.deleteById(commentId);
        postRepo.decrementComment(postId);
    }


    @Transactional
    public CommentsResponseDTO updateComment(Long commentId,Long userId,CommentRequestDTO dto){
        CommentEntity comment = commentRepo.findByIdWithUser(commentId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Yorum bulunamadı"));

        if (!comment.getUser().getId().equals(userId)) {
            throw new CustomExceptions.ForbiddenException("Bu yorumu güncelleme yetkiniz yok.");
        }
        if(!comment.getContents().equals(dto.getContents())) {
            comment.setContents(dto.getContents().trim());
            comment.setUpdateDate(LocalDateTime.now());
        }
        commentRepo.save(comment);

        boolean isLiked = likeRepo.existsByCommentIdAndUserId(commentId, userId);
        return new CommentsResponseDTO(comment,isLiked);
    }

    @Transactional
    public CommentsResponseDTO togglePinComment(Long commentId,Long userId , Long postId){
        CommentEntity comment = commentRepo.findByIdWithUser(commentId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Yorum bulunamadı"));

        if (!comment.getPost().getUser().getId().equals(userId)) {
            throw new CustomExceptions.ForbiddenException("Sadece post sahibi yorumları sabitleyebilir.");
        }
        if (!comment.getPost().getId().equals(postId)) {
            throw new CustomExceptions.BadRequestException("Bu yorum bu posta ait değil!");
        }
        if (comment.isPinned()) {
            comment.setPinned(false);
        } else {
           long currentPinnedCount = commentRepo.countByPostIdAndIsPinnedTrue(postId);
            if (currentPinnedCount >= 3) {
                throw new CustomExceptions.BadRequestException("Maksimum 3 yorum sabitleyebilirsin.");
            }
            comment.setPinned(true);
        }
        commentRepo.save(comment);
        boolean isLiked = likeRepo.existsByCommentIdAndUserId(commentId, userId);
        return new CommentsResponseDTO(comment,isLiked);
    }

    @Transactional
    public LikeResponseDTO toggleLike(Long commentId, Long userId){
        var commentView=commentRepo.findStatsByCommentId(commentId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException("Yorum bulunamadı."));

        accessChecker.checkAccess(commentView.getOwnerId(), userId);
        Optional<Long> existingLike = likeRepo.findIdByCommentIdAndUserId(commentId, userId);
        if(existingLike.isPresent()){
            likeRepo.deleteById(existingLike.get());
            commentRepo.decrementLike(commentId);
            return new LikeResponseDTO(false,commentView.getNumberOfLikes()-1);
        }else{
            UserEntity userRef = entityManager.getReference(UserEntity.class, userId);
            CommentEntity commentRef = entityManager.getReference(CommentEntity.class, commentId);
            LikeEntity like=new LikeEntity();
            like.setComment(commentRef);
            like.setUser(userRef);
            likeRepo.saveAndFlush(like);
            commentRepo.incrementLike(commentId);
            return new LikeResponseDTO(true,commentView.getNumberOfLikes()+1);
        }
    }
}
