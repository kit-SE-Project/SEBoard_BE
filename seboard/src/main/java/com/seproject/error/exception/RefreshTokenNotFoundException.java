package com.seproject.error.exception;

import com.seproject.error.errorCode.ErrorCode;
import lombok.Getter;

import java.util.NoSuchElementException;

@Getter
public class RefreshTokenNotFoundException extends NoSuchElementException {

    private final ErrorCode errorCode;
    public RefreshTokenNotFoundException(ErrorCode errorCode, NoSuchElementException e) {
        super(errorCode.getMessage(), e);
        this.errorCode = ErrorCode.REFRESH_TOKEN_NOT_FOUND;
    }

}