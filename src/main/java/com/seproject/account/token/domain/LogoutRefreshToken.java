package com.seproject.account.token.domain;

import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@AllArgsConstructor
@RedisHash(value = "LOGOUT_REFRESH_TOKEN", timeToLive = 6*60*60)
public class LogoutRefreshToken {

    @Id
    private String refreshToken;
}
