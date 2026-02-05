package com.seproject.error.exception;

import com.seproject.error.errorCode.ErrorCode;

public class NoSuchResourceException extends BusinessLogicException{

    public NoSuchResourceException(ErrorCode errorCode) {
        super(errorCode);
    }
}
