package com.seproject.account.account.service;

import com.seproject.account.account.domain.FormAccount;
import com.seproject.account.account.domain.OAuthAccount;
import com.seproject.account.token.domain.JWT;
import com.seproject.global.IntegrationTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.UUID;

class LogoutServiceTest extends IntegrationTestSupport {
//    @Test
//    public void 로그아웃_성공() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        UsernamePasswordAuthenticationToken token
//                = new UsernamePasswordAuthenticationToken(formAccount.getLoginId(), UUID.randomUUID().toString(),formAccount.getAuthorities());
//
//        JWT accessToken = tokenService.createAccessToken(token);
//        JWT refreshToken = tokenService.createRefreshToken(token);
//
//        logoutService.logout(accessToken,refreshToken);
//        boolean existAccessToken = logoutTokenRepository.existsById(accessToken.getToken());
//        boolean existRefreshToken = logoutRefreshTokenRepository.existsById(refreshToken.getToken());
//
//        Assertions.assertTrue(existAccessToken);
//        Assertions.assertTrue(existRefreshToken);
//    }
//
//    @Test
//    public void 소셜_로그아웃_성공() throws Exception {
//        OAuthAccount oAuthAccount = accountSetup.createOAuthAccount();
//        UsernamePasswordAuthenticationToken token
//                = new UsernamePasswordAuthenticationToken(oAuthAccount.getLoginId(), UUID.randomUUID().toString(),oAuthAccount.getAuthorities());
//
//        JWT accessToken = tokenService.createAccessToken(token);
//        JWT refreshToken = tokenService.createLargeRefreshToken(token);
//
//        logoutService.logout(accessToken,refreshToken);
//        boolean existAccessToken = logoutTokenRepository.existsById(accessToken.getToken());
//        boolean existRefreshToken = logoutLargeRefreshTokenRepository.existsById(refreshToken.getToken());
//
//        Assertions.assertTrue(existAccessToken);
//        Assertions.assertTrue(existRefreshToken);
//    }
//
//
}