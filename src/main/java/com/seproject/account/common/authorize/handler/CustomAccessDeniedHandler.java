package com.seproject.account.common.authorize.handler;

import com.seproject.account.utils.ErrorCodeConverter;
import com.seproject.account.utils.ResponseWriter;
import com.seproject.error.errorCode.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RequiredArgsConstructor
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ResponseWriter responseWriter;
    private final ErrorCodeConverter errorCodeConverter;
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        ErrorCode errorCode = errorCodeConverter.convertExceptionToErrorCode(accessDeniedException);

        if(errorCode != null) {
            responseWriter.write(errorCode,response);
        }
    }
}
