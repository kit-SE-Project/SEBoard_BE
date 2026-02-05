package com.seproject.account.email.domain;

import org.springframework.data.redis.core.RedisHash;


@RedisHash(value = "PASSWORD_CHANGE_EMAIL", timeToLive = 600L)
public class PasswordChangeEmail extends Email {

    public PasswordChangeEmail(String email) {
        super(email);
    }

    public String toMessage(){
        return "비밀번호 변경 인증번호 : " + authToken;
    }

}
