package com.seproject.account.common.authentication.entrypoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seproject.account.utils.ResponseWriter;
import com.seproject.error.Error;
import com.seproject.error.errorCode.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RequiredArgsConstructor
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ResponseWriter responseWriter;
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
//        responseWriter.write(exception, HttpStatus.UNAUTHORIZED,response);
        ErrorCode errorCode = ErrorCode.NOT_LOGIN;

        if(errorCode != null) {
            responseWriter.write(errorCode, response);
        }
    }
}
