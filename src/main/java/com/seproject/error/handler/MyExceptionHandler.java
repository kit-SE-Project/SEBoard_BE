package com.seproject.error.handler;

import com.seproject.error.Error;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.BusinessLogicException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class MyExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Error> handleControllerValidateHandler(
            MethodArgumentNotValidException e){

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        Error error = Error.of(errorCode);
        return new ResponseEntity<>(error,errorCode.getHttpStatus());
    }

    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<Error> handleBusinessLogicException(
            BusinessLogicException e){

        ErrorCode errorCode = e.getErrorCode();
        Error error = Error.of(errorCode);
        return new ResponseEntity<>(error,errorCode.getHttpStatus());
    }

}
