package com.seproject.account.common.authentication.handler.success;

import com.seproject.account.token.domain.JWT;
import com.seproject.account.token.service.TokenService;
import com.seproject.account.utils.ResponseWriter;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.seproject.account.account.controller.dto.LoginDTO.*;

@AllArgsConstructor
@Component
public class FormLoginAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private ResponseWriter responseWriter;
    private TokenService tokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) authentication;

        boolean largeLogin = Boolean.parseBoolean(request.getParameter("largeLogin"));
        JWT accessToken = tokenService.createAccessToken(token);
        JWT refreshToken;

        if(largeLogin) {
            refreshToken = tokenService.createLargeRefreshToken(token);
        } else {
            refreshToken = tokenService.createRefreshToken(token);
        }

        LoginResponseDTO responseDTO = LoginResponseDTO.builder()
                .accessToken(accessToken.getToken())
                .refreshToken(refreshToken.getToken())
                .build();

        responseWriter.write(responseDTO,HttpStatus.OK,response);
    }
}
