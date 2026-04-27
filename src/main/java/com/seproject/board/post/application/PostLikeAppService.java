package com.seproject.board.post.application;

import com.seproject.account.account.domain.Account;
import com.seproject.account.account.domain.repository.AccountRepository;
import com.seproject.board.post.domain.model.LikeType;
import com.seproject.board.post.domain.model.Post;
import com.seproject.board.post.domain.model.PostLike;
import com.seproject.board.post.domain.repository.PostLikeRepository;
import com.seproject.board.post.domain.repository.PostRepository;
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
public class PostLikeAppService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final AccountRepository accountRepository;

    /**
     * 추천 또는 비추천 토글
     * - 같은 타입 재요청 → 취소
     * - 다른 타입 요청 → 기존 반응을 새 타입으로 변경
     *
     * @return 처리 후 최종 반응 상태 (취소 시 null)
     */
    public LikeType toggle(Long postId, String loginId, LikeType requestType) {
        Account account = accountRepository.findByLoginIdWithRole(loginId)
                .orElseThrow(() -> new CustomUserNotFoundException(ErrorCode.USER_NOT_FOUND, null));

        Member member = memberRepository.findByAccountId(account.getAccountId())
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_MEMBER));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_POST));

        Optional<PostLike> existing = postLikeRepository.findByPostIdAndMemberId(postId, member.getBoardUserId());

        if (existing.isEmpty()) {
            PostLike postLike = PostLike.builder()
                    .post(post)
                    .member(member)
                    .likeType(requestType)
                    .build();
            postLikeRepository.save(postLike);
            return requestType;
        }

        PostLike postLike = existing.get();

        if (postLike.getLikeType() == requestType) {
            postLikeRepository.delete(postLike);
            return null;
        }

        postLike.changeLikeType(requestType);
        return requestType;
    }
}