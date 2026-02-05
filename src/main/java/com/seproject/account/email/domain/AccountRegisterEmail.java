package com.seproject.account.email.domain;

import lombok.Builder;
import org.springframework.data.redis.core.RedisHash;

@RedisHash(value = "ACCOUNT_REGISTER_EMAIL", timeToLive = 185L)
public class AccountRegisterEmail extends Email {

    @Builder
    public AccountRegisterEmail(String email) {
        super(email);
    }

    public String toMessage(){
        return "회원가입 인증번호 : " + authToken;
    }

}
