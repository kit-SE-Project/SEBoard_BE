package com.seproject.account.common.authentication.handler.failure;

import com.seproject.account.utils.ErrorCodeConverter;
import com.seproject.account.utils.ResponseWriter;
import com.seproject.error.errorCode.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RequiredArgsConstructor
@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final ResponseWriter responseWriter;
    private final ErrorCodeConverter errorCodeConverter;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        ErrorCode errorCode = errorCodeConverter.convertExceptionToErrorCode(exception);

        if(errorCode != null) {
            responseWriter.write(errorCode, response);
        }
    }



}
