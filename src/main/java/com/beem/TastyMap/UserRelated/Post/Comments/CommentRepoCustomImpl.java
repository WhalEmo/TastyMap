package com.beem.TastyMap.UserRelated.Post.Comments;

import com.beem.TastyMap.RegisterLogin.QUserEntity;
import com.beem.TastyMap.UserRelated.Post.Comments.Like.QLikeEntity;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
public class CommentRepoCustomImpl implements CommentRepoCustom {
    private final JPAQueryFactory queryFactory;

    public CommentRepoCustomImpl(JPAQueryFactory jpaQueryFactory) {
        this.queryFactory = jpaQueryFactory;
    }

    private com.querydsl.core.types.ConstructorExpression<CommentsResponseDTO> commentProjection(QCommentEntity comment, QUserEntity user , QLikeEntity like , Long myId) {
        return Projections.constructor(CommentsResponseDTO.class,
                comment.id,
                comment.parentComment.id,
                comment.post.id,
                comment.contents,
                comment.date,
                comment.numberofLikes,
                user.id,
                user.username,
                user.profile,
                JPAExpressions.selectOne()
                        .from(like)
                        .where(like.comment.id.eq(comment.id).and(like.user.id.eq(myId)))
                        .exists()
        );
    }

    @Override
    public Page<CommentsResponseDTO> getPostComments(Long postId, Long myId,Pageable pageable) {
        QCommentEntity comment = QCommentEntity.commentEntity;
        QUserEntity user = QUserEntity.userEntity;
        QLikeEntity like = QLikeEntity.likeEntity;

        List<CommentsResponseDTO> content = queryFactory
                .select(commentProjection(comment, user, like , myId))
                .from(comment)
                .join(comment.user, user)
                .where(comment.post.id.eq(postId), comment.parentComment.id.isNull())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(comment.date.desc())
                .fetch();

        Long total = queryFactory
                .select(comment.count())
                .from(comment)
                .where(comment.post.id.eq(postId), comment.parentComment.id.isNull())
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<CommentsResponseDTO> getCommentReplys(Long postId,Long myId, Long parentCommentId, Pageable pageable) {
        QCommentEntity comment = QCommentEntity.commentEntity;
        QUserEntity user = QUserEntity.userEntity;
        QLikeEntity like = QLikeEntity.likeEntity;

        List<CommentsResponseDTO> content = queryFactory
                .select(commentProjection(comment, user ,like,myId))
                .from(comment)
                .join(comment.user, user)
                .where(comment.post.id.eq(postId), comment.parentComment.id.eq(parentCommentId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(comment.date.desc())
                .fetch();

        Long total = queryFactory
                .select(comment.count())
                .from(comment)
                .where(comment.post.id.eq(postId), comment.parentComment.id.eq(parentCommentId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}