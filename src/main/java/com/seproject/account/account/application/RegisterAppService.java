package com.seproject.account.account.application;

import com.seproject.account.account.domain.Account;
import com.seproject.account.account.domain.OAuthAccount;
import com.seproject.account.account.service.AccountService;
import com.seproject.account.email.service.RegisterEmailService;
import com.seproject.account.role.domain.Role;
import com.seproject.account.token.domain.JWT;
import com.seproject.account.token.service.TokenService;
import com.seproject.account.role.service.RoleService;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;
import com.seproject.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.seproject.account.account.controller.dto.RegisterDTO.*;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class RegisterAppService {

    private final RegisterEmailService registerEmailService;
    private final AccountService accountService;
    private final RoleService roleService;
    private final MemberService memberService;
    private final TokenService tokenService;

    @Transactional
    public RegisterResponse registerOAuthAccount(OAuth2RegisterRequest request) {
        String email = request.getEmail();
        String nickname = request.getNickname();

        boolean confirmed = registerEmailService.isConfirmed(email);
        boolean existNickname = accountService.isExistNickname(nickname);

        if(!confirmed) {
            throw new CustomIllegalArgumentException(ErrorCode.EMAIL_NOT_FOUNT,null);
        }

        if(existNickname) {
            throw new CustomIllegalArgumentException(ErrorCode.ALREADY_EXIST_NICKNAME,null);
        }

        String name = request.getName();
        String password = request.getPassword();
        String sub = request.getSubject();
        String provider = request.getProvider();

        List<String> roles = new ArrayList<>();
        roles.add(Role.ROLE_USER);

        if(registerEmailService.isKumohMail(email)){
            roles.add(Role.ROLE_KUMOH);
        }

        List<Role> authorities = roleService.findByNameIn(roles);

        Long accountId = accountService.createOAuthAccount(email, name, password, authorities, sub, provider);
        OAuthAccount account = accountService.findOAuthAccountById(accountId);
        Long memberId = memberService.createMember(account,nickname);

        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(account.getSub(), UUID.randomUUID().toString(),account.getAuthorities());
        JWT accessToken = tokenService.createAccessToken(token);
        JWT refreshToken = tokenService.createLargeRefreshToken(token);

        RegisterResponse response = RegisterResponse.builder()
                .accessToken(accessToken.getToken())
                .refreshToken(refreshToken.getToken())
                .build();

        return response;
    }

    @Transactional
    public RegisterResponse registerFormAccount(FormRegisterRequest request) {
        String email = request.getId();
        String nickname = request.getNickname();

        boolean confirmed = registerEmailService.isConfirmed(email);
        boolean existNickname = accountService.isExistNickname(nickname);

        if(!confirmed) {
            throw new CustomIllegalArgumentException(ErrorCode.EMAIL_NOT_FOUNT,null);
        }

        if(existNickname) {
            throw new CustomIllegalArgumentException(ErrorCode.ALREADY_EXIST_NICKNAME,null);
        }

        String name = request.getName();
        String password = request.getPassword();

        List<String> roles = new ArrayList<>();
        roles.add(Role.ROLE_USER);

        if(registerEmailService.isKumohMail(email)){
            roles.add(Role.ROLE_KUMOH);
        }

        List<Role> authorities = roleService.findByNameIn(roles);

        Long accountId = accountService.createFormAccount(email, name, password, authorities);
        Account account = accountService.findById(accountId);
        Long memberId = memberService.createMember(account,nickname);

        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(account.getLoginId(), UUID.randomUUID().toString(),account.getAuthorities());
        JWT accessToken = tokenService.createAccessToken(token);
        JWT refreshToken = tokenService.createLargeRefreshToken(token);

        RegisterResponse response = RegisterResponse.builder()
                .accessToken(accessToken.getToken())
                .refreshToken(refreshToken.getToken())
                .build();

        return response;
    }
}
