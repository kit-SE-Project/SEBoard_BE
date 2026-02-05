package com.seproject.member.domain;



import com.seproject.account.account.domain.Account;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@Table(name = "board_users")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class BoardUser {
    @Id @GeneratedValue
    @Column(name = "board_user_id")
    protected Long boardUserId;
    protected String name;
    @JoinColumn(name = "account_id")
    @OneToOne
    protected Account account;

    public abstract boolean isAnonymous();

    public boolean isOwnAccountId(Long accountId){
        return account.getAccountId().equals(accountId);
    }

    public String getLoginId(){
        return account.getLoginId();
    }

    public void changeName(String name) {
        this.name = name;
    }
}
