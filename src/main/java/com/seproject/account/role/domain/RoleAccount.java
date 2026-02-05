package com.seproject.account.role.domain;

import com.seproject.account.account.domain.Account;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;

@NoArgsConstructor
@Getter
@ToString(exclude = {"account"})
@Entity
public class RoleAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roleAccountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;


    public RoleAccount(Account account, Role role) {
        this.account = account;
        this.role = role;
    }

    public void setAccount(Account account) {
        this.account = account;
    }


}
