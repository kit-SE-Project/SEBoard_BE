package com.seproject.global.data_setup;

import com.seproject.account.account.domain.Account;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityContextSetup {

    public void setSecurityContext(Account account) {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(account, UUID.randomUUID().toString(),account.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(token);
    }
}
