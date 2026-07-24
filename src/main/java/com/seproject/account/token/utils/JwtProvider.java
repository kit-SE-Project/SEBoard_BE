package com.seproject.account.token.utils;

import com.seproject.account.account.domain.Account;
import com.seproject.account.token.domain.JWT;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtProvider {
    @Value("${jwt.secret}")
    private String secret;

    public JWT createLargeRefreshToken(AbstractAuthenticationToken token) {
        Instant instant = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiredDate = instant.plus(JWTProperties.LARGE_REFRESH_TOKEN_EXPIRE, ChronoUnit.SECONDS);
        JWT jwt = createToken(token, JWTProperties.LARGE_REFRESH_TOKEN, JWTProperties.HS256, Date.from(instant), Date.from(expiredDate));
        return jwt;
    }

    public JWT createRefreshToken(AbstractAuthenticationToken token) {
        Instant instant = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiredDate = instant.plus(JWTProperties.REFRESH_TOKEN_EXPIRE, ChronoUnit.SECONDS);
        JWT jwt = createToken(token, JWTProperties.REFRESH_TOKEN, JWTProperties.HS256, Date.from(instant), Date.from(expiredDate));
        return jwt;
    }

    public JWT createAccessToken(AbstractAuthenticationToken token) {
        Instant instant = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiredDate = instant.plus(JWTProperties.ACCESS_TOKEN_EXPIRE, ChronoUnit.SECONDS);
        JWT jwt = createToken(token, JWTProperties.ACCESS_TOKEN, JWTProperties.HS256, Date.from(instant), Date.from(expiredDate));
        return jwt;
    }

    public JWT createToken(AbstractAuthenticationToken token, String type, String alg, Date IssuedAt, Date expiredAt) {
        String jwt = Jwts.builder()
                .setHeaderParam("token_type", type)
                .setHeaderParam(JWTProperties.ALGORITHM, alg)
                .setSubject(extractSubject(token))
                .setIssuedAt(IssuedAt)
                .setExpiration(expiredAt)
                .claim(JWTProperties.AUTHORITIES,token.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .claim("accountId", extractAccountId(token))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();

        return new JWT(jwt);
    }

    private String extractSubject(AbstractAuthenticationToken token) {
        Object principal = token.getPrincipal();
        if (principal instanceof Account) {
            return ((Account) principal).getLoginId();
        }
        return principal.toString();
    }

    private Long extractAccountId(AbstractAuthenticationToken token) {
        Object principal = token.getPrincipal();
        if (principal instanceof Account) {
            return ((Account) principal).getAccountId();
        }
        return null;
    }

}
