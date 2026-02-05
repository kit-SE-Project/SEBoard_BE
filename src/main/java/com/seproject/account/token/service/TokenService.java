package com.seproject.account.token.service;

import com.seproject.account.account.domain.OAuthAccount;
import com.seproject.account.account.domain.repository.OAuthAccountRepository;
import com.seproject.account.token.domain.JWT;
import com.seproject.account.token.domain.LogoutLargeRefreshToken;
import com.seproject.account.token.domain.LogoutRefreshToken;
import com.seproject.account.token.utils.JWTProperties;
import com.seproject.account.token.utils.JwtDecoder;
import com.seproject.account.token.utils.JwtProvider;
import com.seproject.account.account.domain.Account;
import com.seproject.account.account.domain.repository.AccountRepository;
import com.seproject.account.token.domain.repository.LogoutLargeRefreshTokenRepository;
import com.seproject.account.token.domain.repository.LogoutRefreshTokenRepository;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomAuthenticationException;
import com.seproject.error.exception.CustomUserNotFoundException;
import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import static com.seproject.account.token.controller.dto.TokenDTO.*;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class TokenService {

    private final AccountRepository accountRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final JwtProvider jwtProvider;
    private final JwtDecoder jwtDecoder;

    private final LogoutRefreshTokenRepository logoutRefreshTokenRepository;
    private final LogoutLargeRefreshTokenRepository logoutLargeRefreshTokenRepository;


    private boolean existRefreshToken(String refreshToken) {
        return logoutRefreshTokenRepository.existsById(refreshToken) || logoutLargeRefreshTokenRepository.existsById(refreshToken);
    }

    @Transactional
    public AccessTokenRefreshResponse refresh(String refreshToken){

        JWT jwt = new JWT(refreshToken);

        if(existRefreshToken(refreshToken)) {
            throw new CustomAuthenticationException(ErrorCode.DISABLE_REFRESH_TOKEN,null);
        }

        try {
            boolean isLargeToken = isLargeToken(jwt);

            if(isLargeToken) {
                logoutLargeRefreshTokenRepository.save(new LogoutLargeRefreshToken(refreshToken));
            } else {
                logoutRefreshTokenRepository.save(new LogoutRefreshToken(refreshToken));
            }

           String subject = jwtDecoder.getSubject(jwt);
           Account account = accountRepository.findByLoginIdWithRole(subject)
                   .orElseThrow(() -> new CustomUserNotFoundException(ErrorCode.USER_NOT_FOUND,null));

           Collection<? extends GrantedAuthority> authorities = account.getAuthorities();
           UsernamePasswordAuthenticationToken newToken
                   = new UsernamePasswordAuthenticationToken(subject,"",authorities);

           JWT newAccessToken = jwtProvider.createAccessToken(newToken);
           JWT newRefreshToken = isLargeToken ?
                   jwtProvider.createLargeRefreshToken(newToken) : jwtProvider.createRefreshToken(newToken);

           return AccessTokenRefreshResponse.toDTO(newAccessToken.getToken(),newRefreshToken.getToken());
       } catch (CustomAuthenticationException e) {
           throw new CustomAuthenticationException(ErrorCode.DISABLE_REFRESH_TOKEN,e);
       }
    }
    public JWT createAccessToken(UsernamePasswordAuthenticationToken token) {
        return jwtProvider.createAccessToken(token);
    }

    public JWT createRefreshToken(UsernamePasswordAuthenticationToken token) {
        return jwtProvider.createRefreshToken(token);
    }

    public JWT createLargeRefreshToken(UsernamePasswordAuthenticationToken token) {
        return jwtProvider.createLargeRefreshToken(token);
    }

    public Optional<JWT> getAccessToken() {
        return jwtDecoder.getAccessToken();
    }

    public Authentication getAuthentication(JWT token) {
        Claims claims = jwtDecoder.getClaims(token);

        //TODO : SecurityContext에 들어갈 Account, OAuthAccount를 조회하는 방법이 formLogin 방식은 jwt의 sub에 이메일이 들어가고, 카카오는 jwt의 sub에 고유 번호가 들어가서 문제 발생
        Optional<Account> hasAccount = accountRepository.findByLoginIdWithRole(claims.getSubject());

        if(hasAccount.isEmpty()) {
            Optional<OAuthAccount> hasOAuthAccount = oAuthAccountRepository.findOAuthAccountBySubAndProvider(claims.getSubject(), "kakao");

            if(hasOAuthAccount.isPresent()) {
                String loginId = hasOAuthAccount.get().getLoginId();
                hasAccount = accountRepository.findByLoginIdWithRole(loginId);
            }
        }

        if(hasAccount.isPresent()) {
            Account account = hasAccount.get();
            String credential = UUID.randomUUID().toString();
            //TODO: account vs loginId
            return new UsernamePasswordAuthenticationToken(account,credential, account.getAuthorities());
        }

        throw new CustomAuthenticationException(ErrorCode.UNAUTHORIZATION, null);
    }

    public boolean isLargeToken(JWT jwt) {
        JwsHeader header = jwtDecoder.getHeader(jwt);

        try {
            return header
                    .get(JWTProperties.TYPE)
                    .equals(JWTProperties.LARGE_REFRESH_TOKEN);
        } catch (JwtException e) {
            throw new CustomAuthenticationException(ErrorCode.INVALID_JWT, e);
        }
    }

    public void validateToken(JWT jwt) {
        try{
            jwtDecoder.validate(jwt);
        } catch (JwtException e){
            throw new CustomAuthenticationException(ErrorCode.INVALID_JWT, e);
        }
    }
}
