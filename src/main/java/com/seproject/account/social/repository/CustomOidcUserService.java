package com.seproject.account.social.repository;

import com.seproject.account.social.KakaoOidcUser;
import lombok.AllArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class CustomOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        ClientRegistration clientRegistration = userRequest.getClientRegistration();
        clientRegistration = ClientRegistration.withClientRegistration(clientRegistration)
            .userNameAttributeName("sub")
                .build();

        OidcUserRequest oidcUserRequest = new OidcUserRequest(clientRegistration,
                userRequest.getAccessToken(),
                userRequest.getIdToken(),
                userRequest.getAdditionalParameters());

        OidcUserService oidcUserService = new OidcUserService();
        OidcUser oidcUser = oidcUserService.loadUser(oidcUserRequest); // 인가 서버와 통신하여 사용자 정보를 조회 -> OAuth2User 타입 객체에 사용자 정보를 저장

        if(clientRegistration.getRegistrationId().equals("kakao")) {
            return new KakaoOidcUser(oidcUser,clientRegistration);
        }
            throw new OAuth2AuthenticationException("지원하지 않는 open id connection");

    }
}
