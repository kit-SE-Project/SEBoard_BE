package com.seproject.error.exception;

import com.seproject.error.errorCode.ErrorCode;

public class InvalidAuthorizationException extends BusinessLogicException {
    public InvalidAuthorizationException(ErrorCode errorCode) {
        super(errorCode);
    }
}
