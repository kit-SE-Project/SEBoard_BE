package com.seproject.board.comment.persistence;

import com.seproject.board.comment.controller.dto.CommentResponse;
import com.seproject.board.comment.domain.model.Comment;
import com.seproject.board.comment.domain.model.Reply;
import com.seproject.board.comment.domain.repository.CommentSearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentSearchJpaRepository extends CommentSearchRepository {

    @Query("select count(c) from Comment c join Member m on c.author.boardUserId=m.boardUserId where c.author.account.loginId = :loginId and c.isOnlyReadByAuthor = false and c.status= 'NORMAL'")
    Integer countsMemberCommentByLoginId(@Param("loginId") String loginId);

    @Query(value = "select new com.seproject.board.comment.controller.dto.CommentResponse$RetrieveCommentProfileElement(c)" +
            "from Comment c join Member m on c.author.boardUserId=m.boardUserId where c.author.account.loginId = :loginId and c.isOnlyReadByAuthor = false and c.status= 'NORMAL' order by c.baseTime.createdAt desc ",
    countQuery = "select count(c) from Comment c join Member m on c.author.boardUserId=m.boardUserId where c.author.account.loginId = :loginId and c.isOnlyReadByAuthor = false and c.status= 'NORMAL'")
    Page<CommentResponse.RetrieveCommentProfileElement> findMemberCommentByLoginId(@Param("loginId") String loginId, Pageable pagingInfo);
    @Query(value = "select new com.seproject.board.comment.controller.dto.CommentResponse$RetrieveCommentProfileElement(c) from Comment c where c.author.account.loginId = :loginId and c.status= 'NORMAL' order by c.baseTime.createdAt desc",
    countQuery = "select count(c) from Comment c where c.author.account.loginId = :loginId and c.status= 'NORMAL'")
    Page<CommentResponse.RetrieveCommentProfileElement> findCommentByLoginId(@Param("loginId") String loginId, Pageable pagingInfo);
    @Query("select count(c) from Comment c where c.author.account.loginId = :loginId and c.status = 'NORMAL'")
    Integer countsCommentByLoginId(@Param("loginId") String loginId);
    @Query(value = "select * from comments as c where c.post_id = :postId and c.comment_type='comment' order by c.created_at asc", nativeQuery = true)
    Page<Comment> findCommentListByPostId(@Param("postId") Long postId, Pageable pagingInfo);
    @Query("select r from Reply r where r.superComment.commentId = :commentId order by r.baseTime.createdAt asc")
    List<Reply> findReplyListByCommentId(@Param("commentId") Long commentId);
    @Query("select count(r) from Reply r where r.post.postId = :postId and r.status = 'NORMAL'")
    int countReplyByPostId(@Param("postId") Long postId);
}
