package com.seproject.seboard.oauth2;

import com.seproject.seboard.oauth2.service.CustomOAuth2UserService;
import com.seproject.seboard.oauth2.service.CustomOidcUserService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.web.SecurityFilterChain;

@AllArgsConstructor
@EnableWebSecurity
public class OAuth2ClientConfig {

    private final CustomOidcUserService customOidcUserService;
    private final CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {

        return web -> web.ignoring().antMatchers("/static/**");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/").permitAll()
                .antMatchers("/api/user").access("hasAnyRole('SCOPE_profile','SCOPE_email')")
                .antMatchers("/api/oidc").access("hasAnyRole('SCOPE_openid')")
                .anyRequest().authenticated();

        http.oauth2Login(oauth2 -> oauth2.userInfoEndpoint(userInfoEndpointConfig -> userInfoEndpointConfig.userService(customOAuth2UserService)
                .oidcUserService(customOidcUserService)));
        http.logout().logoutSuccessUrl("/");

        return http.build();
    }


    @Bean
    public GrantedAuthoritiesMapper customAuthorityMapper() {
        return new CustomAuthorityMapper();
    }
}