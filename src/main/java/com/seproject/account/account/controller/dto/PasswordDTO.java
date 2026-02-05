package com.seproject.account.account.controller.dto;

import com.seproject.account.account.domain.Account;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

public class PasswordDTO {

    @Data
    public static class ResetPasswordRequest {

        private String email;
        private String password;
    }

    @Builder(access = AccessLevel.PRIVATE)
    @Data
    public static class ResetPasswordResponse {
        private String email;

        public static ResetPasswordResponse toDTO(Account account){
            return builder()
                    .email(account.getLoginId())
                    .build();
        }
    }

    @Data
    public static class PasswordChangeRequest {
        private String nowPassword;
        private String newPassword;
    }
}
