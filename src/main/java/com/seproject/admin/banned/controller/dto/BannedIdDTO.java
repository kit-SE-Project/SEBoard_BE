package com.seproject.admin.banned.controller.dto;

import com.seproject.admin.banned.domain.BannedId;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

public class BannedIdDTO {

    @Data
    @Builder(access = AccessLevel.PRIVATE)
    public static class BannedIdResponse {
        private Long id;
        private String bannedId;

        public BannedIdResponse(Long id, String bannedId) {
            this.id = id;
            this.bannedId = bannedId;
        }

        public static BannedIdResponse toDTO(BannedId bannedId){
            return BannedIdResponse.builder()
                    .id(bannedId.getId())
                    .bannedId(bannedId.getBannedId())
                    .build();
        }
    }

    @Data
    public static class CreateBannedIdRequest {
        private String bannedId;

    }

    @Data
    public static class DeleteBannedIdRequest {
        private String bannedId;

    }
}
