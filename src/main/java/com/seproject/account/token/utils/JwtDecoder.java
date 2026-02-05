package com.seproject.account.token.utils;

import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.seproject.account.account.domain.repository.AccountRepository;
import com.seproject.account.token.domain.JWT;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomAuthenticationException;
import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

import java.util.Optional;

import static com.seproject.account.token.utils.JWTProperties.*;

@RequiredArgsConstructor
@Component
public class JwtDecoder {

    @Value("${jwt.secret}")
    private String secret;
    private final AccountRepository accountRepository;

    public Optional<JWT> getAccessToken(HttpServletRequest request) {
        String jwt = request.getHeader(HEADER_NAME);

        if(StringUtils.hasText(jwt)) {
            return Optional.of(new JWT(jwt));
        }

        return Optional.empty();
    }

    public Optional<JWT> getAccessToken() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if(requestAttributes == null) {
            return Optional.empty();
        }

        HttpServletRequest request = requestAttributes.getRequest();
        return getAccessToken(request);
    }

    public JwsHeader getHeader(JWT jwt) {
        try {
            return Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(jwt.getToken())
                    .getHeader();
        } catch (ExpiredJwtException e) {
            throw new CustomAuthenticationException(ErrorCode.TOKEN_EXPIRED,e);
        }
        catch (JwtException e) {
            throw new CustomAuthenticationException(ErrorCode.INVALID_JWT, e);
        }
    }

    public Claims getClaims(JWT jwt) {
        try {

            Claims body = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(jwt.getToken())
                    .getBody();

            return body;
        } catch (ExpiredJwtException e) {
            throw new CustomAuthenticationException(ErrorCode.TOKEN_EXPIRED,e);
        }
        catch (JwtException e) {
            throw new CustomAuthenticationException(ErrorCode.INVALID_JWT, e);
        }
    }

    public String getSubject(JWT token) {
        Claims claims = getClaims(token);
        return claims.getSubject();
    }

    public void validate(JWT token) {
        Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token.getToken());
    }

}
