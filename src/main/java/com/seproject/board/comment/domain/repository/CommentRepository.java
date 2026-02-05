package com.seproject.board.comment.domain.repository;

import com.seproject.board.comment.domain.model.Comment;
import com.seproject.board.common.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment,Long> {

    @Query("select count(comment) from Comment comment where comment.post.postId = :postId")
    int countCommentsByPostId(@Param("postId") Long postId);

    @Query("select comment from Comment comment where comment.post.postId = :postId")
    List<Comment> findByPostId(@Param("postId") Long postId);

    //List<Comment> findRepliesBySuperCommentId(Long superCommentId);

    @Query("select c from Comment c join c.author join c.author.account where c.author.account.accountId = :accountId")
    List<Comment> findCommentsByAccountId(@Param("accountId") Long accountId);

    @Query("select c from Comment c join c.author join c.author.account where c.author.account.accountId in :accountIds")
    List<Comment> findAllByAccountIds(@Param("accountIds") List<Long> accountIds);


    @Query("select c from Comment c join fetch c.post p join fetch p.category where c.commentId = :commentId")
    Optional<Comment> findCommentWithPostAndCategory(@Param("commentId") Long commentId);

    @Query("select c from Comment c join fetch c.post p join fetch c.author where p.postId = :postId")
    List<Comment> findCommentsWithAuthorByPostId(@Param("postId") Long postId);

    @Modifying
    @Query("update Comment c set c.status = :status where c.commentId in :commentIds")
    int restore(@Param("commentIds") List<Long> commentIds, @Param("status") Status status);

    @Modifying
    @Query("update Comment c set c.status = :status where c.commentId in :commentIds")
    void deleteAllByIds(@Param("commentIds") List<Long> commentIds , @Param("status")Status status);

}
