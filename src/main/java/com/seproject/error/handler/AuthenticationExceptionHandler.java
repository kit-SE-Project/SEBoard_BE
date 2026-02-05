package com.seproject.error.handler;

import com.seproject.error.Error;
import com.seproject.error.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.management.relation.RoleNotFoundException;
import java.util.NoSuchElementException;

@ControllerAdvice
public class AuthenticationExceptionHandler {

    @ExceptionHandler(CustomAccessDeniedException.class)
    public ResponseEntity<Error> handleTokenValidationException(CustomAuthenticationException e) {
        return Error.toResponseEntity(e.getErrorCode());
    }

    @ExceptionHandler(CustomAuthenticationException.class)
    public ResponseEntity<Error> handleAccessTokenExpiredException(CustomAuthenticationException e) {
        return Error.toResponseEntity(e.getErrorCode());
    }

    @ExceptionHandler(CustomIllegalArgumentException.class)
    public ResponseEntity<Error> handleInvalidPage(CustomIllegalArgumentException e) {
        return Error.toResponseEntity(e.getErrorCode());
    }

    @ExceptionHandler(CustomUserNotFoundException.class)
    public ResponseEntity<Error> handleCustomUserNotFoundException(CustomUserNotFoundException e) {
        return Error.toResponseEntity(e.getErrorCode());
    }

    @ExceptionHandler(PasswordIncorrectException.class)
    public ResponseEntity<Error> handlePasswordIncorrect(PasswordIncorrectException e) {
        return Error.toResponseEntity(e.getErrorCode());
    }

    @ExceptionHandler(RefreshTokenNotFoundException.class)
    public ResponseEntity<Error> handleRefreshTokenNotFound(RefreshTokenNotFoundException e) {
        return Error.toResponseEntity(e.getErrorCode());
    }

    @ExceptionHandler(value = {NoSuchElementException.class, IllegalArgumentException.class})
    public ResponseEntity<?> handleUncaughtExceptions(Exception e) {
        return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
