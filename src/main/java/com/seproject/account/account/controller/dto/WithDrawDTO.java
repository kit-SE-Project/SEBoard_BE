package com.seproject.account.account.controller.dto;

import com.seproject.account.account.domain.Account;
import com.seproject.account.role.domain.Role;
import com.seproject.account.role.domain.RoleAccount;
import com.seproject.member.domain.BoardUser;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

public class WithDrawDTO {

    @Data
    public static class WithDrawRequest {
        private String refreshToken;
    }


    @Data
    @Builder(access = AccessLevel.PRIVATE)
    public static class WithDrawResponse {
        private boolean requiredRedirect;
        private String url;
        private Long accountId;
        private Long userId;
        private List<String> roles;
        private String name;

        public static WithDrawResponse toDTO(Account account, BoardUser boardUser, boolean requiredRedirect, String url) {
            return builder()
                    .requiredRedirect(requiredRedirect)
                    .url(url)
                    .accountId(account.getAccountId())
                    .userId(boardUser.getBoardUserId())
                    .roles(account.getRoleAccounts().stream()
                            .map(RoleAccount::getRole)
                            .map(Role::toString)
                            .collect(Collectors.toList()))
                    .name(account.getName())
                    .build();
        }

    }
}
