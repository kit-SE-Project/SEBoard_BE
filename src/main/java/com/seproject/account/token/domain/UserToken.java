package com.seproject.account.token.domain;

import com.seproject.account.account.domain.Account;
import com.seproject.account.account.domain.OAuthAccount;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Builder
@Getter
@RedisHash(value = "USER_TOKEN", timeToLive = 3600L)
public class UserToken {

    @Id
    private String id;

    private Long oAuthAccountId;

}
