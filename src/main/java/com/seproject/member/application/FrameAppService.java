package com.seproject.member.application;

import com.seproject.account.account.domain.Account;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomAuthenticationException;
import com.seproject.error.exception.InvalidAuthorizationException;
import com.seproject.error.exception.NoSuchResourceException;
import com.seproject.member.domain.Member;
import com.seproject.member.domain.model.Frame;
import com.seproject.member.domain.model.MemberFrame;
import com.seproject.member.domain.repository.FrameRepository;
import com.seproject.member.domain.repository.MemberFrameRepository;
import com.seproject.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FrameAppService {

    private final FrameRepository frameRepository;
    private final MemberFrameRepository memberFrameRepository;
    private final MemberService memberService;

    public List<MemberFrame> getMyFrames() {
        Member member = getCurrentMember();
        return memberFrameRepository.findByMemberWithFrame(member);
    }

    @Transactional
    public void equipFrame(Long frameId) {
        Member member = getCurrentMember();
        Frame frame = frameRepository.findById(frameId)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_MENU));

        if (!memberFrameRepository.existsByMemberAndFrame(member, frame)) {
            throw new InvalidAuthorizationException(ErrorCode.ACCESS_DENIED);
        }

        member.equipFrame(frame);
    }

    @Transactional
    public void unequipFrame() {
        Member member = getCurrentMember();
        member.unequipFrame();
    }

    private Member getCurrentMember() {
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN, null));
        return memberService.findByAccountId(account.getAccountId());
    }
}
