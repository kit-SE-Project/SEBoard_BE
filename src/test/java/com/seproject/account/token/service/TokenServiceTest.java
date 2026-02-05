package com.seproject.account.token.service;

import com.seproject.account.account.domain.FormAccount;
import com.seproject.account.token.domain.JWT;
import com.seproject.account.token.utils.JWTProperties;
import com.seproject.error.exception.CustomAuthenticationException;
import com.seproject.global.IntegrationTestSupport;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class TokenServiceTest extends IntegrationTestSupport {
//
//    @Test
//    public void 토큰_파싱_테스트() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        UsernamePasswordAuthenticationToken token =
//                new UsernamePasswordAuthenticationToken(formAccount.getLoginId(),formAccount.getPassword(),formAccount.getAuthorities());
//
//        JWT accessToken = tokenService.createAccessToken(token);
//        Authentication authentication = tokenService.getAuthentication(accessToken);
//
//        Assertions.assertEquals(authentication.getPrincipal(),formAccount);
//    }
//
//    @Test
//    public void 유효기간_지남() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        UsernamePasswordAuthenticationToken token =
//                new UsernamePasswordAuthenticationToken(formAccount.getLoginId(),formAccount.getPassword(),formAccount.getAuthorities());
//
//        Instant instant = Instant.now().minusSeconds(30*60).truncatedTo(ChronoUnit.SECONDS);
//        Instant expiredDate = instant.minus(10*60, ChronoUnit.SECONDS);
//
//        JWT accessToken = jwtProvider.createToken(token, JWTProperties.ACCESS_TOKEN,JWTProperties.HS256, Date.from(instant),Date.from(expiredDate));
//        CustomAuthenticationException customAuthenticationException = Assertions.assertThrows(CustomAuthenticationException.class, () -> {
//            tokenService.validateToken(accessToken);
//        });
//
//        Throwable cause = customAuthenticationException.getCause();
//
//        Assertions.assertTrue(cause instanceof ExpiredJwtException);
//    }
//
//    @Test
//    public void 이상한_문자열() throws Exception {
//        JWT accessToken = new JWT("1234");
//        CustomAuthenticationException customAuthenticationException = Assertions.assertThrows(CustomAuthenticationException.class, () -> {
//            tokenService.validateToken(accessToken);
//        });
//
//        Throwable cause = customAuthenticationException.getCause();
//
//        Assertions.assertTrue(cause instanceof JwtException);
//    }
//
}
