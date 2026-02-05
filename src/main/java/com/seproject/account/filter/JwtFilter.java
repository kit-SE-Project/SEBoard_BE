package com.seproject.account.filter;

import com.seproject.account.common.authentication.handler.failure.CustomAuthenticationFailureHandler;
import com.seproject.account.token.domain.JWT;
import com.seproject.account.token.domain.repository.LogoutTokenRepository;
import com.seproject.account.token.service.TokenService;
import com.seproject.account.token.utils.JwtDecoder;
import com.seproject.error.exception.CustomAuthenticationException;
import com.seproject.error.exception.CustomUserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    private final CustomAuthenticationFailureHandler failureHandler;
    private final LogoutTokenRepository logoutTokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Optional<JWT> getTokenFromService = tokenService.getAccessToken();

        try {
            if(getTokenFromService.isPresent()) {
                JWT token = getTokenFromService.get();
                if(!logoutTokenRepository.existsById(token.getToken())) {
                    Authentication authentication = tokenService.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
            filterChain.doFilter(request,response);
        } catch (CustomAuthenticationException | CustomUserNotFoundException e) {
            failureHandler.onAuthenticationFailure(request,response,e);
        }
    }
}
