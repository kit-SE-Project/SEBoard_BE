package com.seproject.error.exception;

import com.seproject.error.errorCode.ErrorCode;
import lombok.Getter;

@Getter
public class CustomIllegalArgumentException extends IllegalArgumentException {
    private final ErrorCode errorCode;

    public CustomIllegalArgumentException(ErrorCode errorCode, IllegalArgumentException e) {
        super(errorCode.getMessage(),e);
        this.errorCode = errorCode;
    }

    public CustomIllegalArgumentException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
