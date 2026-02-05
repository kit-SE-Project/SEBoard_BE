package com.seproject.error.exception;

import com.seproject.error.errorCode.ErrorCode;

public class InvalidDateException extends BusinessLogicException {
    public InvalidDateException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InvalidDateException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
