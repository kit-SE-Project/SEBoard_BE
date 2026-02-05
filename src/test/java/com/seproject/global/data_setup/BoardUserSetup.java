package com.seproject.global.data_setup;

import com.seproject.account.account.domain.Account;
import com.seproject.member.domain.Anonymous;
import com.seproject.member.domain.Member;
import com.seproject.member.domain.repository.AnonymousRepository;
import com.seproject.member.domain.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class BoardUserSetup {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private AnonymousRepository anonymousRepository;

    public Member createMember(Account account) {

        Member member = Member.builder()
                .name(UUID.randomUUID().toString())
                .account(account)
                .build();

        memberRepository.save(member);

        return member;
    }

    public Anonymous createAnonymous(Account account) {
        Anonymous anonymous = Anonymous.builder()
                .name(account.getName())
                .account(account)
                .build();

        anonymousRepository.save(anonymous);
        return anonymous;
    }
}
