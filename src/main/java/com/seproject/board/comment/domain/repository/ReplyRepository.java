package com.seproject.board.comment.domain.repository;

import com.seproject.board.comment.domain.model.Reply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;

import java.util.List;
import java.util.Optional;

public interface ReplyRepository extends JpaRepository<Reply,Long> {
    @Query("select r from Reply r where r.post.postId = :postId" )
    List<Reply> findByPostId(Long postId);

    @Query("select r from Reply r join fetch r.post p join fetch p.category where r.commentId = :replyId")
    Optional<Reply> findWithPostAndCategory(@Param("replyId") Long replyId);

    @Query("select r from Reply r where r.superComment.commentId = :superCommentId order by r.commentId")
    List<Reply> findBySuperCommentId(@Param("superCommentId") Long superCommentId);
}
