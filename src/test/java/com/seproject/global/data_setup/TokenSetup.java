package com.seproject.global.data_setup;

import com.seproject.account.account.domain.Account;
import com.seproject.account.token.domain.JWT;
import com.seproject.account.token.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Component
public class TokenSetup {

    @Autowired private TokenService tokenService;



    public String getAccessToken(Account account) {
        UsernamePasswordAuthenticationToken token
                = new UsernamePasswordAuthenticationToken(
                account.getLoginId(),account.getPassword(),account.getAuthorities());

        JWT accessToken = tokenService.createAccessToken(token);
        return accessToken.getToken();
    }

}
