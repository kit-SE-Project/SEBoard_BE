package com.seproject.error.exception;

import com.seproject.error.errorCode.ErrorCode;
import lombok.Getter;
import org.springframework.security.authentication.BadCredentialsException;

@Getter
public class PasswordIncorrectException extends BadCredentialsException {

    private ErrorCode errorCode;
    public PasswordIncorrectException(ErrorCode errorCode, BadCredentialsException e) {
        super(errorCode.getMessage(), e);
    }
}
