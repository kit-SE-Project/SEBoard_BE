package com.seproject.error.exception;

import com.seproject.error.errorCode.ErrorCode;

public class DuplicatedReportException extends BusinessLogicException {
    public DuplicatedReportException(ErrorCode errorCode) {
        super(errorCode);
    }
}
