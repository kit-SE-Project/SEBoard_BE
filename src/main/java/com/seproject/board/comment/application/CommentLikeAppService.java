package com.seproject.board.comment.application;

import com.seproject.account.account.domain.Account;
import com.seproject.account.account.domain.repository.AccountRepository;
import com.seproject.board.comment.domain.model.Comment;
import com.seproject.board.comment.domain.model.CommentLike;
import com.seproject.board.comment.domain.repository.CommentLikeRepository;
import com.seproject.board.comment.service.CommentService;
import com.seproject.board.post.domain.model.LikeType;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomUserNotFoundException;
import com.seproject.error.exception.NoSuchResourceException;
import com.seproject.member.domain.Member;
import com.seproject.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentLikeAppService {

    private final CommentLikeRepository commentLikeRepository;
    private final CommentService commentService;
    private final MemberRepository memberRepository;
    private final AccountRepository accountRepository;

    public LikeType toggle(Long commentId, String loginId, LikeType requestType) {
        Account account = accountRepository.findByLoginIdWithRole(loginId)
                .orElseThrow(() -> new CustomUserNotFoundException(ErrorCode.USER_NOT_FOUND, null));

        Member member = memberRepository.findByAccountId(account.getAccountId())
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_MEMBER));

        Comment comment = commentService.findWithPostAndCategory(commentId);

        Optional<CommentLike> existing = commentLikeRepository.findByCommentIdAndMemberId(commentId, member.getBoardUserId());

        if (existing.isEmpty()) {
            CommentLike commentLike = CommentLike.builder()
                    .comment(comment)
                    .member(member)
                    .likeType(requestType)
                    .build();
            commentLikeRepository.save(commentLike);
            return requestType;
        }

        CommentLike commentLike = existing.get();

        if (commentLike.getLikeType() == requestType) {
            commentLikeRepository.delete(commentLike);
            return null;
        }

        commentLike.changeLikeType(requestType);
        return requestType;
    }
}