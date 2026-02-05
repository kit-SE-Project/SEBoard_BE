package com.seproject.error.exception;

import com.seproject.error.errorCode.ErrorCode;
import lombok.Getter;

@Getter
public abstract class AbstractErrorException extends RuntimeException{
    protected final ErrorCode errorCode;

    public AbstractErrorException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AbstractErrorException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

}
