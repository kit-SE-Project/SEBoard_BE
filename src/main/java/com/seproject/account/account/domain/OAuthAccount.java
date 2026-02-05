package com.seproject.account.account.domain;

import com.seproject.account.role.domain.Role;
import com.seproject.account.role.domain.RoleAccount;
import com.seproject.board.common.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@DiscriminatorValue(value = "OAUTH_ACCOUNT")
@Entity
@Table(name="oauth_accounts")
public class OAuthAccount extends Account{

    private String provider;

    @Column(unique = true,nullable = true)
    private String sub;

    @Builder
    public OAuthAccount(Long accountId, String loginId,
                        String name,
                        String password, List<RoleAccount> roleAccounts, LocalDateTime createdAt, Status status, String provider, String sub) {
        this.accountId = accountId;
        this.loginId = loginId;
        this.name = name;
        this.password = password;

        this.roleAccounts = new ArrayList<>();
        for (RoleAccount roleAccount : roleAccounts) {
            addRoleAccount(roleAccount);
        }

        this.createdAt = createdAt;
        this.status = status;
        this.provider = provider;
        this.sub = sub;
    }

    public void removeSub(){
        this.sub = null;
    }


}
