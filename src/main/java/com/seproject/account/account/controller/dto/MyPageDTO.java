package com.seproject.account.account.controller.dto;

import com.seproject.account.account.domain.Account;
import com.seproject.member.domain.Member;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;

public class MyPageDTO {

    @Data
    @Builder(access = AccessLevel.PRIVATE)
    public static class MyInfoResponse {
        private String nickname;
        private Long userId;
        private String email;
        private List<String> roles;

        public static MyInfoResponse toDTO(Member findMember,Account account, List<String> roles) {
            return builder()
                    .nickname(findMember.getName())
                    .email(account.getLoginId())
                    .roles(roles)
                    .userId(findMember.getBoardUserId())
                    .build();
        }
    }

    @Data
    public static class MyInfoChangeRequest {
        private String nickname;

    }

    @Data
    @Builder(access = AccessLevel.PRIVATE)
    public static class MyInfoChangeResponse {
        private String nickname;

        public static MyInfoChangeResponse toDTO(Member member) {
            return builder()
                    .nickname(member.getName())
                    .build();
        }
    }
}
