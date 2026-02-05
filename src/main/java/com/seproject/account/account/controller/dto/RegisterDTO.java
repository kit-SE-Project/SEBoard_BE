package com.seproject.account.account.controller.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class RegisterDTO {

    @Data
    public static class OAuth2RegisterRequest {
        private String subject;
        private String provider;
        private String email;
        private String password;
        private String name;
        private String nickname;
    }


    @Data
    public static class FormRegisterRequest {
        private String id;
        private String password;
        private String nickname;
        private String name;
    }

    @Data
    @Builder
    public static class RegisterResponse {
        private String accessToken;
        private String refreshToken;
    }

    @Data
    public static class ConfirmDuplicateNicknameRequest {
        private String nickname;
    }

    @Data
    @Builder
    public static class ConfirmDuplicateNicknameResponse {
        private boolean isDuplication;

        public static ConfirmDuplicateNicknameResponse toDTO(boolean isDuplication) {
            return builder()
                    .isDuplication(isDuplication)
                    .build();
        }
    }


}
