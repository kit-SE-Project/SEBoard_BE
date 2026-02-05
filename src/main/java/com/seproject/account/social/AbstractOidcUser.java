package com.seproject.account.social;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class AbstractOidcUser implements OidcUser {

    private ClientRegistration registration;
    private Map<String, Object> claims;
    private OidcIdToken idToken;
    private OidcUserInfo userInfo;
    private Map<String,Object> attributes;
    private List<? extends GrantedAuthority> authorities;

    public AbstractOidcUser(OidcUser oidcUser, ClientRegistration registration) {
        this.registration = registration;
        attributes = oidcUser.getAttributes();
        authorities = oidcUser.getAuthorities().stream()
                .filter(authority -> authority.getAuthority().startsWith("ROLE_"))
                .map(authority -> new SimpleGrantedAuthority(authority.getAuthority()))
                .collect(Collectors.toList());

        this.claims = oidcUser.getClaims();
        this.idToken = oidcUser.getIdToken();
        this.userInfo = oidcUser.getUserInfo();
    }

    public List<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public String getPassword() {
        return UUID.randomUUID().toString();
    }

    public abstract String getId();
    public abstract String getProvider();
    public abstract String getEmail();
    public abstract String getProfile();

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Map<String, Object> getClaims() {
        return claims;
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return userInfo;
    }

    @Override
    public OidcIdToken getIdToken() {
        return idToken;
    }

}
