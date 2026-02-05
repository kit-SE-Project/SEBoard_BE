package com.seproject.account.account.domain;

import com.seproject.account.role.domain.Role;
import com.seproject.account.role.domain.RoleAccount;
import com.seproject.board.common.Status;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Entity
@DiscriminatorValue(value = "FORM_ACCOUNT")
@Table(name = "form_accounts")
public class FormAccount extends Account {

    @Builder
    public FormAccount(Long accountId, String loginId,
                   String name,
                   String password, List<RoleAccount> roleAccounts,LocalDateTime createdAt,Status status) {
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
    }

    public static FormAccount createFormAccount(Long accountId,String loginId,String name,String nickname,String password,LocalDateTime createdAt,Status status) {
        return builder()
                .accountId(accountId)
                .loginId(loginId)
                .name(name)
                .password(password)
                .createdAt(createdAt)
                .status(status)
                .build();
    }
}
