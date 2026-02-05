package com.seproject.account.utils;

import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class ErrorCodeConverter {

    public ErrorCode convertExceptionToErrorCode(AuthenticationException e) {

        if(e instanceof PasswordIncorrectException) {
            return ErrorCode.PASSWORD_INCORRECT;
        } else if(e instanceof CustomAuthenticationException) {
            return ((CustomAuthenticationException)e).getErrorCode();
        } else if(e instanceof CustomUserNotFoundException){
            return ((CustomUserNotFoundException)e).getErrorCode();
        }

        return null;
    }

    public ErrorCode convertExceptionToErrorCode(AccessDeniedException e) {

        if(e instanceof CustomAccessDeniedException) {
            return ((CustomAccessDeniedException)e).getErrorCode();
        } else if(e != null){
            return ErrorCode.ACCESS_DENIED;
        }

        return null;
    }
}
