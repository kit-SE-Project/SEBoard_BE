package com.seproject.admin.banned.controller.dto;

import com.seproject.admin.banned.domain.BannedNickname;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

public class BannedNicknameDTO {

    @Data
    public static class BannedNicknameResponse {
        private Long id;
        private String bannedNickname;

        public BannedNicknameResponse(Long id, String bannedNickname) {
            this.id = id;
            this.bannedNickname = bannedNickname;
        }
    }

    @Data
    public static class CreateBannedNicknameRequest {
        private String nickname;
    }

    @Data
    public static class DeleteBannedNicknameRequest {
        private String nickname;
    }

}
