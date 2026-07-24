package com.seproject.board.comment.persistence;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.seproject.board.comment.domain.model.Comment;
import com.seproject.board.common.Status;
import com.seproject.board.post.domain.model.LikeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.seproject.board.comment.domain.model.QComment.comment;
import static com.seproject.board.comment.domain.model.QCommentLike.commentLike;

@Repository
@RequiredArgsConstructor
public class CommentQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public List<Comment> findBestCommentsByPostId(Long postId, int limit) {
        return jpaQueryFactory
                .select(comment)
                .from(comment)
                .leftJoin(commentLike).on(
                        commentLike.comment.eq(comment)
                                .and(commentLike.likeType.eq(LikeType.LIKE))
                )
                .where(comment.post.postId.eq(postId))
                .where(comment.status.eq(Status.NORMAL))
                .groupBy(comment.commentId)
                .having(commentLike.count().gt(0))
                .orderBy(commentLike.count().desc())
                .limit(limit)
                .fetch();
    }
}