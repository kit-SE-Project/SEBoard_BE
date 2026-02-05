package com.seproject.error.exception;

import com.seproject.error.errorCode.ErrorCode;

public class ExceedFileSizeException extends BusinessLogicException {
    public ExceedFileSizeException(ErrorCode errorCode) {
        super(errorCode);
    }
}
