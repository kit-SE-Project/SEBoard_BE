package com.seproject.account.common.authentication.handler.success;

import com.seproject.account.account.domain.OAuthAccount;
import com.seproject.account.account.domain.repository.OAuthAccountRepository;
import com.seproject.account.social.KakaoOidcUser;
import com.seproject.account.social.TemporalUserInfo;
import com.seproject.account.social.repository.TemporalUserInfoRepository;
import com.seproject.account.token.domain.UserToken;
import com.seproject.account.token.domain.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;


@RequiredArgsConstructor
@Component
public class OidcAuthenticationSuccessHandler implements AuthenticationSuccessHandler {


    @Value("${frontend.url}")
    private String REDIRECT_URL;

    private final OAuthAccountRepository oAuthAccountRepository;
    private final TemporalUserInfoRepository temporalUserInfoRepository;
    private final UserTokenRepository userTokenRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String jwt;
        OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken)authentication;
        KakaoOidcUser oidcUser = (KakaoOidcUser) oAuth2AuthenticationToken.getPrincipal();

        Optional<OAuthAccount> oAuthAccountBySubAndProvider = oAuthAccountRepository.findOAuthAccountBySubAndProvider(oidcUser.getId(), oidcUser.getProvider());
        if(oAuthAccountBySubAndProvider.isPresent()) {

            OAuthAccount oAuthAccount = oAuthAccountBySubAndProvider.get();
            String id = UUID.randomUUID().toString();
            UserToken userToken = UserToken.builder()
                    .id(id)
                    .oAuthAccountId(oAuthAccount.getAccountId())
                    .build();

            userTokenRepository.save(userToken);
            response.sendRedirect(REDIRECT_URL + "/login?id=" + id );
        } else {
            String email = oidcUser.getEmail();

            String id = UUID.randomUUID().toString();

            TemporalUserInfo temporalUserInfo = TemporalUserInfo.builder()
                    .id(id)
                    .subject(oidcUser.getId())
                    .provider(oidcUser.getProvider())
                    .email(email)
                    .name(oidcUser.getName())
                    .nickname(oidcUser.getNickName())
                    .build();

            temporalUserInfoRepository.save(temporalUserInfo);
            response.sendRedirect(REDIRECT_URL + "/signup/oauth?id=" + id);
        }
    }

}

