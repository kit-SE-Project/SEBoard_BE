package com.seproject.account.model.account;

import com.seproject.account.model.account.Account;
import com.seproject.account.model.role.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@DiscriminatorValue(value = "OAUTH_ACCOUNT")
@Entity
@Table(name="oauth_accounts")
public class OAuthAccount extends Account{

    private String provider;

    @Column(unique = true)
    private String sub;

    @Builder
    public OAuthAccount(Long accountId, String loginId,
                   String name, String nickname,
                   String password, List<Role> authorities,String provider, String sub) {
        this.accountId = accountId;
        this.loginId = loginId;
        this.name = name;
        this.nickname = nickname;
        this.password = password;
        this.authorities = authorities;
        this.createdAt = LocalDateTime.now();
        this.isDeleted = false;
        this.provider = provider;
        this.sub = sub;
    }
}