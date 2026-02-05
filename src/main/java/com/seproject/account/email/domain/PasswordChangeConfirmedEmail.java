package com.seproject.account.email.domain;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@RedisHash(value = "PASSWORD_CHANGE_CONFIRMED_EMAIL", timeToLive = 600L)
public class PasswordChangeConfirmedEmail {

    @Id
    private String email;

    @Builder
    public PasswordChangeConfirmedEmail(String email) {
        this.email = email;
    }

}
