package com.seproject.board.comment.domain.repository;

import com.seproject.board.comment.controller.dto.CommentResponse.RetrieveCommentProfileElement;
import com.seproject.board.comment.domain.model.Comment;
import com.seproject.board.comment.domain.model.Reply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;

@NoRepositoryBean
public interface CommentSearchRepository extends Repository<Comment, Long> {
    Integer countsMemberCommentByLoginId(String loginId);

    Page<RetrieveCommentProfileElement> findMemberCommentByLoginId(String loginId, Pageable pagingInfo);
    Page<RetrieveCommentProfileElement> findCommentByLoginId(String loginId, Pageable pagingInfo);
    Integer countsCommentByLoginId(String loginId);
    Page<Comment> findCommentListByPostId(Long postId, Pageable pagingInfo);
    List<Reply> findReplyListByCommentId(Long commentId);
    int countReplyByPostId(Long postId);
}
