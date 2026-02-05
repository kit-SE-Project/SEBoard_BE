package com.seproject.admin.account.controller.dto;

import com.seproject.account.account.domain.Account;
import com.seproject.admin.role.controller.dto.RoleDTO;
import com.seproject.member.domain.BoardUser;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdminAccountDto {

    @Data
    @NoArgsConstructor
    public static class AccountResponse {
        private Long accountId;
        private Long userId;
        private String loginId;
        private String name;
        private String nickname;
        private LocalDateTime registeredDate;
        private List<RoleDTO.RoleResponse> roles;

        public AccountResponse(Long accountId,
                               Long userId,
                               String loginId,
                               String name,
                               String nickname,
                               LocalDateTime registeredDate) {
            this.accountId = accountId;
            this.userId = userId;
            this.loginId = loginId;
            this.name = name;
            this.nickname = nickname;
            this.registeredDate = registeredDate;
            this.roles = new ArrayList<>();
        }

        @Builder(access = AccessLevel.PRIVATE)
        public AccountResponse(Long accountId,
                               Long userId,
                               String name,
                               String nickname,
                               LocalDateTime registeredDate,
                               List<RoleDTO.RoleResponse> roles) {
            this.accountId = accountId;
            this.userId = userId;
            this.name = name;
            this.nickname = nickname;
            this.registeredDate = registeredDate;
            this.roles = roles;
        }

        public static AccountResponse of(Account account, BoardUser boardUser, List<RoleDTO.RoleResponse> roles) {

            return builder()
                    .accountId(account.getAccountId())
                    .userId(boardUser.getBoardUserId())
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
