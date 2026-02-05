package com.seproject.account.account.controller.dto;

import com.seproject.account.account.domain.Account;
import com.seproject.account.role.domain.Role;
import com.seproject.account.role.domain.RoleAccount;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;


public class KumohAuthDTO {



    @Data
    public static class KumohAuthRequest {
        private String email;
    }

    @Builder(access = AccessLevel.PRIVATE)
    @Data
    public static class KumohAuthResponse {
        private String email;
        private List<String> authorities;

        public static KumohAuthResponse toDTO(Account account){
            return builder()
                    .email(account.getLoginId())
                    .authorities(account.getRoles().stream()
                            .map(Role::getAuthority)
                            .collect(Collectors.toList()))
                    .build();
        }
    }

}
