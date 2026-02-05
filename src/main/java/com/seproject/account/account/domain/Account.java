package com.seproject.account.account.domain;

import com.seproject.account.role.domain.Role;
import com.seproject.account.role.domain.RoleAccount;
import com.seproject.board.common.Status;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@DiscriminatorColumn(name = "dtype")
@Inheritance(strategy = InheritanceType.JOINED)
@Entity
@Table(name = "accounts")
public abstract class Account implements UserDetails {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long accountId;

    protected String loginId;
    protected String name;
    protected String password;

    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY,cascade = CascadeType.ALL,orphanRemoval = true)
    protected List<RoleAccount> roleAccounts = new ArrayList<>();
    protected LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    protected Status status = Status.NORMAL;

    public void update(String loginId,String password,String name,List<RoleAccount> roleAccounts) {
        this.loginId = loginId;
        this.name = name;
        this.roleAccounts.clear();
        this.roleAccounts.addAll(roleAccounts);
        if(StringUtils.hasText(password)){
            this.password = password;
        }
    }

    public void addRoleAccount(RoleAccount roleAccount) {
        this.roleAccounts.add(roleAccount);
        roleAccount.setAccount(this);
    }

    public String changePassword(String password) {
        this.password = password;
        return password;
    }

    public void delete(boolean isPermanent) {
        if(isPermanent) {
            status = Status.PERMANENT_DELETED;
        } else {
            status = Status.TEMP_DELETED;
        }
    }

    public String getUsername() {
        return loginId;
    }

    public List<Role> getRoles() {
        return roleAccounts.stream().map(RoleAccount::getRole).collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getRoles();
    }

    public void restore() {
        status = Status.NORMAL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(getAccountId(), account.getAccountId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAccountId());
    }
}
