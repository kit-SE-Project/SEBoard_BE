package com.seproject.account.email.domain;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;


@Getter
@RedisHash(value = "ACCOUNT_REGISTER_CONFIRMED_EMAIL", timeToLive = 600L)
public class AccountRegisterConfirmedEmail {

    @Id
    private String email;

    @Builder
    public AccountRegisterConfirmedEmail(String email) {
        this.email = email;
    }


}
