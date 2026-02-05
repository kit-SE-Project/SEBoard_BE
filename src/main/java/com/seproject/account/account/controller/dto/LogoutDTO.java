package com.seproject.account.account.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

public class LogoutDTO {

    @Data
    public static class LogoutRequestDTO {
        private String refreshToken;
    }

    @Data
    @AllArgsConstructor
    public static class LogoutResponseDTO {
        private boolean requiredRedirect;
        private String url;
    }

}
