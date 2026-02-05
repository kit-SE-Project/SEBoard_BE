package com.seproject.error;

import com.seproject.error.errorCode.ErrorCode;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.ResponseEntity;

@Builder
@Data
public class Error {
    private int code;
    private String message;
    public static ResponseEntity<Error> toResponseEntity(ErrorCode errorCode) {
        Error e = Error.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return new ResponseEntity<>(e,errorCode.getHttpStatus());
    }

    public static Error of(ErrorCode errorCode) {
        return Error.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }
}
