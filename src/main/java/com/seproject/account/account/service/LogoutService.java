package com.seproject.account.account.service;


import com.seproject.account.token.domain.JWT;
import com.seproject.account.token.service.TokenService;
import com.seproject.account.token.utils.JwtDecoder;
import com.seproject.account.token.domain.LogoutLargeRefreshToken;
import com.seproject.account.token.domain.LogoutRefreshToken;
import com.seproject.account.token.domain.LogoutToken;
import com.seproject.account.token.domain.repository.LogoutLargeRefreshTokenRepository;
import com.seproject.account.token.domain.repository.LogoutRefreshTokenRepository;
import com.seproject.account.token.domain.repository.LogoutTokenRepository;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomAuthenticationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class LogoutService {

    private static final String LOGOUT_URL = "https://kauth.kakao.com/oauth/logout?client_id=";
    private static final String REDIRECT_PATH = "&logout_redirect_uri=https://seboard.site/login";
    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;

    private final LogoutTokenRepository logoutTokenRepository;
    private final LogoutRefreshTokenRepository logoutRefreshTokenRepository;
    private final LogoutLargeRefreshTokenRepository logoutLargeRefreshTokenRepository;
    private final TokenService tokenService;

    public String getRedirectURL() {
        return LOGOUT_URL + clientId + REDIRECT_PATH;
    }

    public void logout(JWT accessToken, JWT refreshToken) {
        logoutTokenRepository.save(new LogoutToken(accessToken.getToken()));

        String refreshTokenValue = refreshToken.getToken();

        if(tokenService.isLargeToken(refreshToken)) {
            logoutLargeRefreshTokenRepository.save(new LogoutLargeRefreshToken(refreshTokenValue));
        } else {
            logoutRefreshTokenRepository.save(new LogoutRefreshToken(refreshTokenValue));
        }
    }

}
