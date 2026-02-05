package com.seproject.error.exception;

import com.seproject.error.errorCode.ErrorCode;

public abstract class BusinessLogicException extends AbstractErrorException{
    public BusinessLogicException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BusinessLogicException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
