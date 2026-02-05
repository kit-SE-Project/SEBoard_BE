package com.seproject.error.exception;

import com.seproject.error.errorCode.ErrorCode;
import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

@Getter
public class CustomAuthenticationException extends AuthenticationException {

    private final ErrorCode errorCode;
    public CustomAuthenticationException(ErrorCode errorCode,Throwable e) {
        super(errorCode.getMessage(),e);
        this.errorCode = errorCode;
    }
}
