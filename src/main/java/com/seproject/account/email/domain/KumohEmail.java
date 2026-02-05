package com.seproject.account.email.domain;

import org.springframework.data.redis.core.RedisHash;

@RedisHash(value = "KUMOH_EMAIL", timeToLive = 600L)
public class KumohEmail extends Email{
    public KumohEmail(String email) {
        super(email);
    }

    @Override
    public String toMessage(){
        return "정회원 전환 인증번호 : " + authToken;
    }
}
