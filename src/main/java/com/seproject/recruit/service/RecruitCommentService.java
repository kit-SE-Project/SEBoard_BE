package com.seproject.recruit.service;

import com.seproject.account.account.domain.Account;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.board.menu.domain.model.RecruitMenu;
import com.seproject.board.menu.domain.repository.RecruitMenuRepository;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomAuthenticationException;
import com.seproject.error.exception.CustomIllegalArgumentException;
import com.seproject.error.exception.InvalidAuthorizationException;
import com.seproject.member.domain.Member;
import com.seproject.member.service.MemberService;
import com.seproject.recruit.domain.model.RecruitComment;
import com.seproject.recruit.domain.model.RecruitPost;
import com.seproject.recruit.domain.repository.RecruitCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitCommentService {

    private final RecruitCommentRepository recruitCommentRepository;
    private final RecruitPostService recruitPostService;
    private final RecruitMenuRepository recruitMenuRepository;
    private final MemberService memberService;

    public List<RecruitComment> findComments(Long recruitPostId) {
        return recruitCommentRepository
                .findByRecruitPostIdAndParentCommentIsNullOrderByCreatedAtAsc(recruitPostId);
    }

    public List<RecruitComment> findReplies(Long parentCommentId) {
        return recruitCommentRepository.findByParentCommentIdOrderByCreatedAtAsc(parentCommentId);
    }

    @Transactional
    public RecruitComment create(Long recruitPostId, Long accountId, String contents,
                                  Long parentCommentId, boolean isReadOnlyAuthor) {
        RecruitPost post = recruitPostService.findById(recruitPostId);
        Member author = memberService.findByAccountId(accountId);

        RecruitComment parent = null;
        Long tagCommentId = null;

        if (parentCommentId != null) {
            RecruitComment targetComment = findById(parentCommentId);
            if (targetComment.getParentComment() != null) {
                // 답글에 답글: 루트 댓글을 parent로, 원래 답글을 tag
                parent = targetComment.getParentComment();
                tagCommentId = targetComment.getId();
            } else {
                parent = targetComment;
            }
        }

        return recruitCommentRepository.save(RecruitComment.builder()
                .recruitPost(post).author(author).contents(contents)
                .parentComment(parent).tagCommentId(tagCommentId)
                .isReadOnlyAuthor(isReadOnlyAuthor)
                .build());
    }

    @Transactional
    public RecruitComment update(Long id, Long accountId, String contents, boolean isReadOnlyAuthor) {
        RecruitComment comment = findById(id);
        if (!comment.isOwner(accountId)) throw new IllegalArgumentException("수정 권한이 없습니다.");
        comment.update(contents, isReadOnlyAuthor);
        return comment;
    }

    /**
     * 일반 Post CommentAppService.removeComment 와 동일 구조:
     *  - 로그인 확인
     *  - 작성자 or manage 권한 확인
     *  - 소프트 삭제
     *  - 루트 댓글이면 답글도 소프트 삭제 (RecruitComment는 삭제된 댓글 placeholder UI 없음)
     */
    @Transactional
    public void delete(Long id) {
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN, null));

        RecruitComment comment = findById(id);
        RecruitMenu menu = recruitMenuRepository.findWithAuthorizations()
                .orElseThrow(() -> new CustomIllegalArgumentException(ErrorCode.NOT_EXIST_MENU, null));

        if (!comment.isOwner(account.getAccountId()) && !menu.manageable(account.getRoles())) {
            throw new InvalidAuthorizationException(ErrorCode.ACCESS_DENIED);
        }

        comment.softDelete();
    }

    public RecruitComment findById(Long id) {
        RecruitComment comment = recruitCommentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        if (comment.isDeleted()) throw new IllegalArgumentException("삭제된 댓글입니다.");
        return comment;
    }
}
