package com.seproject.account.email.controller.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

public class EmailDTO {

    @Data
    public static class EmailAuthenticationRequest {
        private String email;
    }

    @Data
    public static class EmailConfirmRequest {
        private String email;
        private String authToken;
    }


    @Data
    @Builder(access = AccessLevel.PRIVATE)
    public static class EmailConfirmResponse {

        private String name;
        private String email;


        public static EmailConfirmResponse toDTO(String name,String email) {
            return builder()
                    .name(name)
                    .email(email)
                    .build();
        }
    }

}
