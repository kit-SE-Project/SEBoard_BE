package com.seproject.error.exception;

import com.seproject.error.errorCode.ErrorCode;
import lombok.Getter;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Getter
public class CustomUserNotFoundException extends UsernameNotFoundException {

    private final ErrorCode errorCode;

    public CustomUserNotFoundException(ErrorCode errorCode,UsernameNotFoundException e) {
        super(errorCode.getMessage(),e);
        this.errorCode = errorCode;
    }

}
