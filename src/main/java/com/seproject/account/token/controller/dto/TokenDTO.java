package com.seproject.account.token.controller.dto;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

public class TokenDTO {


    @Builder(access = AccessLevel.PRIVATE)
    @Data
    public static class TokenResponse {
        private AccessToken accessToken;
        private RefreshToken refreshToken;

        public static TokenResponse toDTO(AccessToken accessToken,RefreshToken refreshToken) {
            return builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        }
    }

    @Data
    public static class AccessTokenRefreshRequest {
        private String refreshToken;
    }

    @Builder
    @Data
    public static class AccessTokenRefreshResponse {
        private String accessToken;
        private String refreshToken;

        public static AccessTokenRefreshResponse toDTO(String accessToken,String refreshToken) {
            return builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        }
    }
}
