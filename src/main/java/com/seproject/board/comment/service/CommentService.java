package com.seproject.board.comment.service;

import com.seproject.board.comment.domain.model.Comment;
import com.seproject.board.comment.domain.repository.CommentRepository;
import com.seproject.board.common.Status;
import com.seproject.board.post.domain.model.Post;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.NoSuchResourceException;
import com.seproject.member.domain.BoardUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    @Transactional
    public Long createComment(Post post,
                              BoardUser boardUser,
                              String contents,
                              boolean isOnlyReadByAuthor) {
        Comment comment = post.writeComment(contents, boardUser, isOnlyReadByAuthor);
        commentRepository.save(comment);
        return comment.getCommentId();
    }

    @Transactional
    public int updateStatus(List<Long> commentIds,Status status) {
        int cnt = commentRepository.restore(commentIds, status);
        return cnt;
    }


    public List<Comment> findAllByIds(List<Long> commentIds) {
        List<Comment> comments = commentRepository.findAllById(commentIds);
        return comments;
    }

    public Comment findById(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_COMMENT));
        return comment;
    }

    public List<Comment> findWithAuthorByPostId(Long postId) {
        List<Comment> comments = commentRepository.findCommentsWithAuthorByPostId(postId);
        return comments;
    }

    public Comment findWithPostAndCategory(Long commentId) {
        Comment comment = commentRepository.findCommentWithPostAndCategory(commentId)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_COMMENT));
        return comment;
    }
}
