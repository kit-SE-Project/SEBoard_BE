package com.seproject.account.account.controller.dto;

import lombok.Builder;
import lombok.Data;


public class LoginDTO {

    @Data
    @Builder
    public static class LoginResponseDTO{
        private String accessToken;
        private String refreshToken;
    }

    @Data
    @Builder
    public static class TemporalLoginResponseDTO {
        private String subject;
        private String provider;
        private String email;
        private String name;
        private String nickname;
        private String accessToken;
    }
}
