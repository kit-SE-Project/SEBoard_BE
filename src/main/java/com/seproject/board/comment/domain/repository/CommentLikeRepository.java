package com.seproject.board.comment.domain.repository;

import com.seproject.board.comment.domain.model.CommentLike;
import com.seproject.board.post.domain.model.LikeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    @Query("select cl from CommentLike cl where cl.comment.commentId = :commentId and cl.member.boardUserId = :memberId")
    Optional<CommentLike> findByCommentIdAndMemberId(@Param("commentId") Long commentId, @Param("memberId") Long memberId);

    @Query("select count(cl) from CommentLike cl where cl.comment.commentId = :commentId and cl.likeType = :likeType")
    int countByCommentIdAndLikeType(@Param("commentId") Long commentId, @Param("likeType") LikeType likeType);
}