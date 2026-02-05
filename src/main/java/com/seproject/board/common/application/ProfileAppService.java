package com.seproject.board.common.application;

import com.seproject.account.account.domain.Account;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.InvalidAuthorizationException;
import com.seproject.error.exception.NoSuchResourceException;
import com.seproject.board.comment.controller.dto.CommentResponse.RetrieveCommentProfileElement;
import com.seproject.board.post.controller.dto.PostResponse.RetrievePostListResponseElement;
import com.seproject.board.common.controller.dto.ProfileResponse.ProfileInfoResponse;
import com.seproject.board.comment.domain.repository.CommentRepository;
import com.seproject.board.comment.domain.repository.CommentSearchRepository;
import com.seproject.board.post.domain.repository.BookmarkRepository;
import com.seproject.board.post.domain.repository.PostSearchRepository;
import com.seproject.member.domain.Member;
import com.seproject.member.domain.repository.MemberRepository;
import com.seproject.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProfileAppService {
    private final PostSearchRepository postSearchRepository;
    private final CommentRepository commentRepository;
    private final CommentSearchRepository commentSearchRepository;
    private final BookmarkRepository bookmarkRepository;

    private final MemberService memberService;

    public ProfileInfoResponse retrieveProfileInfo(Long memberId){
        Account account = SecurityUtils.getAccount().orElse(null);

        //TODO : SQL 쿼리 변경 필요
        Integer postCount = null;
        Integer commentCount = null;
        Integer bookmarkCount = null;

        Member member = memberService.findByIdWithAccount(memberId);
        Account memberAccount = member.getAccount();
        String nickname = member.getName();

        String loginId = memberAccount.getLoginId();

        if(account != null && account.equals(memberAccount)) {
            postCount = postSearchRepository.countsPostByLoginId(loginId);
            commentCount = commentSearchRepository.countsCommentByLoginId(loginId);
            bookmarkCount = bookmarkRepository.countsBookmarkByLoginId(loginId);
        } else {
            postCount = postSearchRepository.countsMemberPostByLoginId(loginId);
            commentCount = commentSearchRepository.countsMemberCommentByLoginId(loginId);
        }

        return ProfileInfoResponse.builder()
                .nickname(nickname)
                .postCount(postCount)
                .commentCount(commentCount)
                .bookmarkCount(bookmarkCount)
                .build();
    }

    public Page<RetrievePostListResponseElement> retrieveMyPost(Long memberId, int page, int perPage){
        Account account = SecurityUtils.getAccount().orElse(null);

        Page<RetrievePostListResponseElement> posts;
        Member member = memberService.findByIdWithAccount(memberId);
        Account memberAccount = member.getAccount();

        if(account == null || !account.equals(memberAccount)){
            String loginId = memberAccount.getLoginId();
            posts = postSearchRepository.findMemberPostByLoginId(loginId, PageRequest.of(page, perPage));
        } else {
            String loginId = memberAccount.getLoginId();
            posts = postSearchRepository.findPostByLoginId(loginId, PageRequest.of(page, perPage));
        }

        posts.getContent().forEach(post -> {
            int commentSize = commentRepository.countCommentsByPostId(post.getPostId());
            post.setCommentSize(commentSize);
        });

        return posts;
    }

    public Page<RetrievePostListResponseElement> retrieveBookmarkPost(Long memberId, int page, int perPage){
        Member member = memberService.findByIdWithAccount(memberId);
        Account account = member.getAccount();
        String loginId = account.getLoginId();

        Page<RetrievePostListResponseElement> posts = postSearchRepository
                .findBookmarkPostByLoginId(loginId, PageRequest.of(page, perPage));

        posts.getContent().forEach(post -> {
            int commentSize = commentRepository.countCommentsByPostId(post.getPostId());
            post.setCommentSize(commentSize);
        });

        return posts;
    }

    public Page<RetrieveCommentProfileElement> retrieveMyComment(Long memberId, int page, int perPage){
        Account account = SecurityUtils.getAccount().orElse(null);

        Member member = memberService.findByIdWithAccount(memberId);
        Account memberAccount = member.getAccount();
        String loginId = memberAccount.getLoginId();

        if(account == null || !account.getLoginId().equals(loginId)){
            return commentSearchRepository
                    .findMemberCommentByLoginId(loginId, PageRequest.of(page, perPage));
        }else{
            return commentSearchRepository
                    .findCommentByLoginId(loginId, PageRequest.of(page, perPage));
        }
    }

}
