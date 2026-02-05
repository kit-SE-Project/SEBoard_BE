package com.seproject.error.exception;

import com.seproject.error.errorCode.ErrorCode;

public class InvalidFileExtensionException extends BusinessLogicException {
    public InvalidFileExtensionException(ErrorCode errorCode) {
        super(errorCode);
    }
}
