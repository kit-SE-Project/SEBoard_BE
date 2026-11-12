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

        String tokenValue = jwt.getToken();
        if (tokenValue == null || tokenValue.isBlank() || existRefreshToken(tokenValue)) {
            throw new CustomAuthenticationException(ErrorCode.DISABLE_REFRESH_TOKEN,null);
        }

        try {
            var signed = jwtDecoder.getSignedClaims(jwt);
            Object type = signed.getHeader().get("token_type");
            boolean isLargeToken = JWTProperties.LARGE_REFRESH_TOKEN.equals(type);
            if (signed.getBody().getExpiration() == null
                    || (!isLargeToken && !JWTProperties.REFRESH_TOKEN.equals(type))) {
                throw new CustomAuthenticationException(ErrorCode.DISABLE_REFRESH_TOKEN, null);
            }

            Claims claims = signed.getBody();
            Account account = findAccountBySubject(claims.getSubject())
                    .orElseThrow(() -> new CustomUserNotFoundException(ErrorCode.USER_NOT_FOUND, null));
            Object accountId = claims.get("accountId");
            if (accountId != null && !account.getAccountId().toString().equals(accountId.toString())) {
                throw new CustomAuthenticationException(ErrorCode.DISABLE_REFRESH_TOKEN, null);
            }

            if (isLargeToken) {
                logoutLargeRefreshTokenRepository.save(new LogoutLargeRefreshToken(tokenValue));
            } else {
                logoutRefreshTokenRepository.save(new LogoutRefreshToken(tokenValue));
            }

           Collection<? extends GrantedAuthority> authorities = account.getAuthorities();
           UsernamePasswordAuthenticationToken newToken
                   = new UsernamePasswordAuthenticationToken(account,"",authorities);

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

        Optional<Account> hasAccount = findAccountBySubject(claims.getSubject());

        if(hasAccount.isPresent()) {
            Account account = hasAccount.get();
            String credential = UUID.randomUUID().toString();
            //TODO: account vs loginId
            return new UsernamePasswordAuthenticationToken(account,credential, account.getAuthorities());
        }

        throw new CustomAuthenticationException(ErrorCode.UNAUTHORIZATION, null);
    }

    private Optional<Account> findAccountBySubject(String subject) {
        return accountRepository.findByLoginIdWithRole(subject).or(() ->
                oAuthAccountRepository.findOAuthAccountBySubAndProvider(subject, "kakao")
                        .map(OAuthAccount::getLoginId).flatMap(accountRepository::findByLoginIdWithRole));
    }

    public boolean isLargeToken(JWT jwt) {
        JwsHeader header = jwtDecoder.getHeader(jwt);

        try {
            return header
                    .get("token_type")
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
