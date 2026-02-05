package com.seproject.error.exception;

import com.seproject.error.errorCode.ErrorCode;
import lombok.Getter;
import org.springframework.security.access.AccessDeniedException;

@Getter
public class CustomAccessDeniedException extends AccessDeniedException {
    private final ErrorCode errorCode;
    public CustomAccessDeniedException(ErrorCode errorCode,Throwable e) {
        super(errorCode.getMessage(),e);
        this.errorCode = errorCode;
    }



}
