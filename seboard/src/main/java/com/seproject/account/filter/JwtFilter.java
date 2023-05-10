package com.seproject.account.filter;

import com.seproject.account.authentication.handler.failure.CustomAuthenticationFailureHandler;
import com.seproject.account.model.token.AccessToken;
import com.seproject.account.service.TokenService;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.account.jwt.JwtDecoder;
import com.seproject.error.exception.CustomAuthenticationException;
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

@RequiredArgsConstructor
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;

    private final CustomAuthenticationFailureHandler failureHandler;
    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String jwt = jwtDecoder.getAccessToken(request);

        try {
            if(StringUtils.hasText(jwt)) {

//                if(jwtDecoder.isTemporalToken(jwt)) {
//                    throw new CustomAuthenticationException(ErrorCode.NOT_REGISTERED_USER,null);
//                }

                AccessToken accessToken = tokenService.findAccessToken(jwt);

                if(accessToken != null) {
                    Authentication authentication = jwtDecoder.getAuthentication(accessToken);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

            }
            filterChain.doFilter(request,response);
        } catch (CustomAuthenticationException e) {
            failureHandler.onAuthenticationFailure(request,response,e);
        }
    }
}
