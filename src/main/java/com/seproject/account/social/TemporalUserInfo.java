package com.seproject.account.social;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Builder
@Getter
@RedisHash(value = "USER_INFO", timeToLive = 3600L)
public class TemporalUserInfo {

    @Id
    private String id;
    private String subject;
    private String provider;
    private String email;
    private String name;
    private String nickname;

}
