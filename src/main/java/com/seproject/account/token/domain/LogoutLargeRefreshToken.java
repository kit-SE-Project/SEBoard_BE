package com.seproject.account.token.domain;

import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@AllArgsConstructor
@RedisHash(value = "LOGOUT_LARGE_REFRESH_TOKEN", timeToLive = 30*24*60*60)
public class LogoutLargeRefreshToken {

    @Id
    private String refreshToken;
}
