package com.seproject.member.service;


import com.seproject.account.account.domain.Account;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;
import com.seproject.error.exception.NoSuchResourceException;
import com.seproject.member.domain.Member;
import com.seproject.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class MemberService {

    private final MemberRepository memberRepository;
    @Transactional
    public Long createMember(Account account,String nickname) {
        Member member = Member.builder()
                .name(nickname)
                .account(account)
                .build();

        memberRepository.save(member);

        return member.getBoardUserId();
    }

    public Member findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_MEMBER));
    }

    public Member findByIdWithAccount(Long id) {
        return memberRepository.findByIdWithAccount(id)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_MEMBER));
    }



    public Member findByLoginId(String loginId) {
        return memberRepository.findByLoginId(loginId)
                .orElseThrow(()-> new NoSuchResourceException(ErrorCode.NOT_EXIST_MEMBER));

    }

    public Member findByAccountId(Long accountId) {
        return memberRepository.findByAccountId(accountId)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_MEMBER));
    }
}
