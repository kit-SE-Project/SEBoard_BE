package com.seproject.account.account.application;

import com.seproject.account.account.controller.dto.MyPageDTO.MyInfoChangeRequest;
import com.seproject.account.account.controller.dto.MyPageDTO.MyInfoChangeResponse;
import com.seproject.account.account.controller.dto.MyPageDTO.MyInfoResponse;
import com.seproject.account.account.controller.dto.PasswordDTO.ResetPasswordResponse;
import com.seproject.account.account.domain.Account;
import com.seproject.account.account.service.AccountService;
import com.seproject.account.account.service.LogoutService;
import com.seproject.account.email.service.KumohEmailService;
import com.seproject.account.email.service.PasswordChangeEmailService;
import com.seproject.account.role.domain.Role;
import com.seproject.account.role.domain.RoleAccount;
import com.seproject.account.role.service.RoleService;
import com.seproject.account.token.domain.JWT;
import com.seproject.account.token.service.TokenService;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomAuthenticationException;
import com.seproject.error.exception.CustomIllegalArgumentException;
import com.seproject.member.domain.Member;
import com.seproject.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.seproject.account.account.controller.dto.KumohAuthDTO.*;
import static com.seproject.account.account.controller.dto.LogoutDTO.*;
import static com.seproject.account.account.controller.dto.PasswordDTO.*;
import static com.seproject.account.account.controller.dto.WithDrawDTO.*;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class AccountAppService {

    private final TokenService tokenService;
    private final AccountService accountService;
    private final RoleService roleService;
    private final LogoutService logoutService;
    private final PasswordChangeEmailService passwordChangeEmailService;
    private final KumohEmailService kumohEmailService;

    @Transactional
    public LogoutResponseDTO logout(LogoutRequestDTO request) {
        JWT accessToken = tokenService.getAccessToken()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN, null));
        JWT refreshToken = new JWT(request.getRefreshToken());
        tokenService.validateToken(refreshToken);

        logoutService.logout(accessToken,refreshToken);

        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN,null));
        if(isOAuthUser(account)) {
            String redirectURL = logoutService.getRedirectURL();
            return new LogoutResponseDTO(true,redirectURL);
        }

        return new LogoutResponseDTO(false,null);
    }

    @Transactional
    public WithDrawResponse withDraw(WithDrawRequest request) {

        JWT accessToken = tokenService.getAccessToken()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN, null));
        JWT refreshToken = new JWT(request.getRefreshToken());
        tokenService.validateToken(refreshToken);

        logoutService.logout(accessToken,refreshToken);

        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN,null));

        Long accountId = account.getAccountId();
        Member member = memberService.findByAccountId(accountId);

        if(isOAuthUser(account)) {
            String redirectURL = logoutService.getRedirectURL();
            accountService.deleteAccount(accountId, true);
            return WithDrawResponse.toDTO(account,member,true,redirectURL);
        }

        accountService.deleteAccount(accountId, true);
        return WithDrawResponse.toDTO(account,member,false,null);
    }

    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {

        if(!passwordChangeEmailService.isConfirmed(request.getEmail())) {
            throw new CustomIllegalArgumentException(ErrorCode.EMAIL_NOT_FOUNT,null);
        }

        Long accountId = accountService.resetPassword(request.getEmail(),request.getPassword());
        Account account = accountService.findById(accountId);
        return ResetPasswordResponse.toDTO(account);
    }

    @Transactional
    public void changePassword(PasswordChangeRequest request) {
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN,null));

        String nowPassword = request.getNowPassword();
        if(!accountService.matchPassword(account, nowPassword)) {
            throw new CustomIllegalArgumentException(ErrorCode.PASSWORD_INCORRECT,null);
        }

        String newPassword = request.getNewPassword();
        accountService.changePassword(account, newPassword);
    }

    @Transactional
    public KumohAuthResponse addKumohRole(KumohAuthRequest request) {

        String email = request.getEmail();

        if(!kumohEmailService.isConfirmed(email)) {
            throw new CustomIllegalArgumentException(ErrorCode.EMAIL_NOT_FOUNT,null);
        }

        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN, null));

        account = accountService.findById(account.getAccountId());
        List<Role> authorities = account.getRoles();

        boolean flag = false;
        for (Role authority : authorities) {
            flag |= authority.getAuthority().equals(Role.ROLE_KUMOH);
        }

        if(flag) {
            throw new CustomIllegalArgumentException(ErrorCode.ALREADY_KUMOH,null);
        }

        Role kumoh = roleService.findByName(Role.ROLE_KUMOH);
        account.addRoleAccount(new RoleAccount(account,kumoh));

        return KumohAuthResponse.toDTO(account);
    }

    private final MemberService memberService;

    public MyInfoResponse findMyPage() {
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN,null));

        Member findMember = memberService.findByAccountId(account.getAccountId());

        return MyInfoResponse.toDTO(findMember,account, account.getRoles().stream()
                .map(Role::getAuthority)
                .collect(Collectors.toList()));
    }

    @Transactional
    public MyInfoChangeResponse updateMyInfo(MyInfoChangeRequest request) {
        Account account = SecurityUtils.getAccount()
                        .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN,null));
        Member findMember = memberService.findByAccountId(account.getAccountId());
        account = accountService.findById(account.getAccountId());
        findMember.changeName(request.getNickname());

        return MyInfoChangeResponse.toDTO(findMember);
    }


    private boolean isOAuthUser(Account account) {
        String principal = account.getUsername();

        if(!StringUtils.isEmpty(principal) && accountService.isOAuthUser(principal)) {
            return true;
        }

        return false;
    }
}
