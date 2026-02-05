package com.seproject.account.token.domain;

import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@AllArgsConstructor
@RedisHash(value = "LOGOUT_TOKEN" , timeToLive = 30*60)
public class LogoutToken {
    @Id
    private String accessToken;
}
