package com.seproject.account.email.domain;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash(value = "KUMOH_CONFIRMED_EMAIL", timeToLive = 185L)
public class KumohConfirmedEmail {
    @Id
    private String email;

    @Builder
    public KumohConfirmedEmail(String email) {
        this.email = email;
    }
}
