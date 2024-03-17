package com.seproject.admin.account.controller.dto;

import com.seproject.account.account.domain.Account;
import com.seproject.admin.role.controller.dto.RoleDTO;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdminAccountDto {

    @Data

    public static class AccountResponse {
        private Long accountId;
        private String loginId;
        private String name;
        private String nickname;
        private LocalDateTime registeredDate;
        private List<RoleDTO.RoleResponse> roles;

        public AccountResponse(Long accountId,
                               String loginId,
                               String name,
                               String nickname,
                               LocalDateTime registeredDate) {
            this.accountId = accountId;
            this.loginId = loginId;
            this.name = name;
            this.nickname = nickname;
            this.registeredDate = registeredDate;
            this.roles = new ArrayList<>();
        }

        @Builder(access = AccessLevel.PRIVATE)
        public AccountResponse(Long accountId,
                               String loginId,
                               String name,
                               String nickname,
                               LocalDateTime registeredDate,
                               List<RoleDTO.RoleResponse> roles) {
            this.accountId = accountId;
            this.loginId = loginId;
            this.name = name;
            this.nickname = nickname;
            this.registeredDate = registeredDate;
            this.roles = roles;
        }

        public static AccountResponse of(Account account,List<RoleDTO.RoleResponse> roles) {

            return builder()
                    .accountId(account.getAccountId())
                    .loginId(account.getLoginId())
                    .name(account.getName())
                    .registeredDate(account.getCreatedAt())
                    .roles(roles)
                    .build();

        }

    }


    @Data
    public static class CreateAccountRequest {
        private String id;
        private String password;
        private String name;
        private String nickname;
        private List<Long> roles;
    }

    @Data
    public static class UpdateAccountRequest {
        private String id;
        private String password;
        private String name;
        private String nickname;
        private List<Long> roles;
    }

    @Data
    public static class DeleteAccountRequest {
        List<Long> accountIds;
    }

    @Data
    public static class RestoreAccountRequest {
        List<Long> accountIds;
    }

}
