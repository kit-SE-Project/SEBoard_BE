package com.seproject.account.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seproject.error.Error;
import com.seproject.error.errorCode.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RequiredArgsConstructor
@Component
public class ResponseWriter {

    public static final String APPLICATION_JSON_VALUE = "application/json";
    public static final String UTF_8_VALUE = "UTF-8";

    private interface WriteCallback<E> {
        E call() throws IOException;
    }
    private final ObjectMapper objectMapper = new ObjectMapper();

    private void jsonTemplate(WriteCallback<String> callback,HttpServletResponse response) throws IOException {
        response.setContentType(APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(UTF_8_VALUE);
        String result = callback.call();
        response.getWriter().write(result);
    }

    public void write(Object object,
                      HttpStatus httpStatus,
                      HttpServletResponse response) throws IOException {

        jsonTemplate(() -> {
            String result = objectMapper.writeValueAsString(object);
            response.setStatus(httpStatus.value());
            return result;
        },response);

    }

    public void write(ErrorCode errorCode, HttpServletResponse response) throws IOException {
        Error error = Error.of(errorCode);

        jsonTemplate(() -> {
            String result = objectMapper.writeValueAsString(error);
            response.setStatus(errorCode.getHttpStatus().value());
            return result;
        },response);
    }

}
