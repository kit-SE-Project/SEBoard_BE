package com.seproject.account.token.utils;

import com.seproject.account.account.domain.FormAccount;
import com.seproject.account.token.domain.JWT;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomAuthenticationException;
import com.seproject.global.IntegrationTestSupport;
import com.seproject.global.data_setup.AccountSetup;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtDecoderTest extends IntegrationTestSupport {
//
//    @Test
//    public void 토큰_획득() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        UsernamePasswordAuthenticationToken token
//                = new UsernamePasswordAuthenticationToken(formAccount.getLoginId(),formAccount.getPassword(),formAccount.getAuthorities());
//        JWT accessToken = jwtProvider.createAccessToken(token);
//        MockHttpServletRequest request = new MockHttpServletRequest();
//        request.addHeader("Authorization",accessToken.getToken());
//        Optional<JWT> jwt = jwtDecoder.getAccessToken(request);
//        Assertions.assertEquals(jwt.get().getToken(),accessToken.getToken());
//    }
//
//    @Test
//    public void 토큰이_존재하지_않음() throws Exception {
//        Optional<JWT> jwt = jwtDecoder.getAccessToken();
//        Assertions.assertTrue(jwt.isEmpty());
//    }
//
//    @Test
//    public void 토큰이_빈상태() throws Exception {
//        MockHttpServletRequest request = new MockHttpServletRequest();
//        request.addHeader("Authorization","");
//        Optional<JWT> jwt = jwtDecoder.getAccessToken(request);
//        Assertions.assertTrue(jwt.isEmpty());
//    }
//
//    @Test
//    public void 헤더_파싱() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        UsernamePasswordAuthenticationToken token
//                = new UsernamePasswordAuthenticationToken(formAccount.getLoginId(),formAccount.getPassword(),formAccount.getAuthorities());
//        JWT accessToken = jwtProvider.createAccessToken(token);
//
//        JwsHeader header = jwtDecoder.getHeader(accessToken);
//
//        String alg = header.getAlgorithm();
//        String type = header.getType();
//        Assertions.assertEquals(alg,JWTProperties.HS256);
//        Assertions.assertEquals(type,JWTProperties.ACCESS_TOKEN);
//    }
//
//    @Test
//    public void 만료_토큰_헤더_파싱() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        UsernamePasswordAuthenticationToken token
//                = new UsernamePasswordAuthenticationToken(formAccount.getLoginId(),formAccount.getPassword(),formAccount.getAuthorities());
//        Instant instant = Instant.now().minusSeconds(30*60).truncatedTo(ChronoUnit.SECONDS);
//        Instant expiredAt = Instant.now().minusSeconds(10*60).truncatedTo(ChronoUnit.SECONDS);
//
//        JWT accessToken = jwtProvider.createToken(token,JWTProperties.ACCESS_TOKEN,JWTProperties.HS256, Date.from(instant),Date.from(expiredAt));
//
//        CustomAuthenticationException customAuthenticationException = assertThrows(CustomAuthenticationException.class, () -> {
//            jwtDecoder.getHeader(accessToken);
//        });
//        Assertions.assertEquals(customAuthenticationException.getErrorCode(), ErrorCode.TOKEN_EXPIRED);
//
//        Throwable cause = customAuthenticationException.getCause();
//        Assertions.assertTrue(cause instanceof ExpiredJwtException);
//    }
//
//    @Test
//    public void 이상한_토큰_헤더_파싱() throws Exception {
//        String token = "eyJ0eXBlIjoibGFyZ2VSZWZyZXNoVG9rZW4iLCJhbGciOiJIUzI1NiJ9" +
//                ".eyJzdWIiOiJiZTUwOWUzYS05NDFjLTQ3MWQtOTRkZC04YWFkMzg5YmU1M2IiLCJpYXQiOjE2ODk1MTA0MTMsImV4cCI6MTc4OTUxMDQwOCwiYXV0aG9yaXRpZXMiOlsiUk9MRV9VU0VSIl19" +
//                ".4pasu8oHV3iVyxDOqAxGA2HwGFPJ6M4_KM75msHD1B0";
//
//        JWT jwt = new JWT(token);
//        CustomAuthenticationException customAuthenticationException = assertThrows(CustomAuthenticationException.class, () -> {
//            jwtDecoder.getHeader(jwt);
//        });
//        Assertions.assertEquals(customAuthenticationException.getErrorCode(),ErrorCode.INVALID_JWT);
//
//        Assertions.assertTrue(customAuthenticationException.getCause() instanceof JwtException);
//    }
//
//    @Test
//    public void claim_파싱() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(formAccount.getLoginId(), formAccount.getPassword(), formAccount.getAuthorities());
//        Instant instant = Instant.now().plusSeconds(10*60).truncatedTo(ChronoUnit.SECONDS);
//        Instant expiredAt = Instant.now().plusSeconds(30*60).truncatedTo(ChronoUnit.SECONDS);
//
//        JWT accessToken = jwtProvider.createToken(token,JWTProperties.ACCESS_TOKEN,JWTProperties.HS256,Date.from(instant),Date.from(expiredAt));
//        Claims claims = jwtDecoder.getClaims(accessToken);
//
//        Assertions.assertEquals(formAccount.getLoginId(),claims.get("sub"));
//        Assertions.assertEquals(String.valueOf(instant.getEpochSecond()),String.valueOf(claims.get("iat")));
//        Assertions.assertEquals(String.valueOf(expiredAt.getEpochSecond()),String.valueOf(claims.get("exp")));
//        List<String> authorities = (List<String>) claims.get("authorities");
//        Assertions.assertEquals(1,authorities.size());
//        Assertions.assertEquals("ROLE_USER",authorities.get(0));
//    }
//
//    @Test
//    public void claim_파싱_만료_토큰() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(formAccount.getLoginId(), formAccount.getPassword(), formAccount.getAuthorities());
//        Instant instant = Instant.now().minusSeconds(30*60).truncatedTo(ChronoUnit.SECONDS);
//        Instant expiredAt = Instant.now().minusSeconds(10*60).truncatedTo(ChronoUnit.SECONDS);
//
//        JWT accessToken = jwtProvider.createToken(token,JWTProperties.ACCESS_TOKEN,JWTProperties.HS256,Date.from(instant),Date.from(expiredAt));
//        CustomAuthenticationException customAuthenticationException = assertThrows(CustomAuthenticationException.class, () -> {
//            Claims claims = jwtDecoder.getClaims(accessToken);
//        });
//
//        Assertions.assertEquals(customAuthenticationException.getErrorCode(),ErrorCode.TOKEN_EXPIRED);
//        Throwable cause = customAuthenticationException.getCause();
//        Assertions.assertTrue(cause instanceof ExpiredJwtException);
//    }
//
//    @Test
//    public void claim_파싱_invalid_토큰() throws Exception {
//        String token = "eyJ0eXBlIjoibGFyZ2VSZWZyZXNoVG9rZW4iLCJhbGciOiJIUzI1NiJ9" +
//                ".eyJzdWIiOiJiZTUwOWUzYS05NDFjLTQ3MWQtOTRkZC04YWFkMzg5YmU1M2IiLCJpYXQiOjE2ODk1MTA0MTMsImV4cCI6MTc4OTUxMDQwOCwiYXV0aG9yaXRpZXMiOlsiUk9MRV9VU0VSIl19" +
//                ".4pasu8oHV3iVyxDOqAxGA2HwGFPJ6M4_KM75msHD1B0";
//        JWT accessToken = new JWT(token);
//        CustomAuthenticationException customAuthenticationException = assertThrows(CustomAuthenticationException.class, () -> {
//            Claims claims = jwtDecoder.getClaims(accessToken);
//        });
//
//        Assertions.assertEquals(customAuthenticationException.getErrorCode(),ErrorCode.INVALID_JWT);
//        Throwable cause = customAuthenticationException.getCause();
//        Assertions.assertTrue(cause instanceof JwtException);
//    }
//
//    @Test
//    public void subject_테스트() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(formAccount.getLoginId(), formAccount.getPassword(), formAccount.getAuthorities());
//        Instant instant = Instant.now().plusSeconds(10*60).truncatedTo(ChronoUnit.SECONDS);
//        Instant expiredAt = Instant.now().plusSeconds(30*60).truncatedTo(ChronoUnit.SECONDS);
//
//        JWT accessToken = jwtProvider.createToken(token,JWTProperties.ACCESS_TOKEN,JWTProperties.HS256,Date.from(instant),Date.from(expiredAt));
//        String subject = jwtDecoder.getSubject(accessToken);
//
//        Assertions.assertEquals(formAccount.getLoginId(),subject);
//    }
//
//
//
//
//
//
//
//
//
//
//
}