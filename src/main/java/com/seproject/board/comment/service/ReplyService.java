package com.seproject.board.comment.service;

import com.seproject.board.comment.domain.model.Comment;
import com.seproject.board.comment.domain.model.Reply;
import com.seproject.board.comment.domain.repository.ReplyRepository;
import com.seproject.board.post.domain.model.Post;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;
import com.seproject.error.exception.NoSuchResourceException;
import com.seproject.member.domain.Anonymous;
import com.seproject.member.domain.BoardUser;
import com.seproject.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReplyService {

    private final ReplyRepository replyRepository;

    @Transactional
    public Long createReply(Comment superComment,
                            String contents,
                            Comment tagComment,
                            BoardUser member, boolean onlyReadByAuthor) {

        Post superCommentPost = superComment.getPost();
        Post tagCommentPost = tagComment.getPost();

        if (!superCommentPost.getPostId().equals(tagCommentPost.getPostId()) )
            throw new CustomIllegalArgumentException(ErrorCode.DIFFERENT_POST_COMMENT,null);

        Comment c = tagComment;

        while (c instanceof Reply) {
            c = ((Reply) c).getSuperComment();
        }

        if (!c.getCommentId().equals(superComment.getCommentId())) {
            throw new CustomIllegalArgumentException(ErrorCode.DIFFERENT_SUPER_COMMENT,null);
        }

        Reply reply = superComment.writeReply(contents, tagComment, member, onlyReadByAuthor);
        replyRepository.save(reply);

        return reply.getCommentId();
    }

    public Reply findWithPostAndCategory(Long id) {
        Reply reply = replyRepository.findWithPostAndCategory(id)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_COMMENT));
        return reply;
    }

    public Reply findById(Long id) {
        Reply reply = replyRepository.findById(id)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_COMMENT));
        return reply;
    }

    public List<Reply> findBySuperCommendId(Long superCommentId) {
        return replyRepository.findBySuperCommentId(superCommentId);
    }

}
