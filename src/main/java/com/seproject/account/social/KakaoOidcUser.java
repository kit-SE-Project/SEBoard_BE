package com.seproject.account.social;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public class KakaoOidcUser extends AbstractOidcUser
{
    public KakaoOidcUser(OidcUser oidcUser, ClientRegistration registration) {
        super(oidcUser,registration);
    }

    @Override
    public String getId() {return (String)getAttributes().get("sub");}
    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getEmail() {
        return (String)getAttributes().get("email");
    }

    @Override
    public String getName() {
        return (String)getAttributes().get("nickname");
    }

    @Override
    public String getProfile() {
        return (String)getAttributes().get("picture");
    }





}
