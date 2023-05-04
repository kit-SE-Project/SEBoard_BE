package com.seproject.account.model;

import lombok.*;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "accounts")
public class Account implements UserDetails {

    @Id @GeneratedValue
    private Long accountId;

    @Column(name = "login_id",unique = true)
    private String loginId;
    private String username;
    private String nickname;
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name="authorities",
            joinColumns={@JoinColumn(name="account_id", referencedColumnName="accountId")},
            inverseJoinColumns={@JoinColumn(name="role_id", referencedColumnName="roleId")})
    private List<Role> authorities;

    private LocalDateTime createdAt;

    @Builder
    public Account(Long accountId, String loginId,
                   String username, String nickname,
                   String password, List<Role> authorities, LocalDateTime createdAt) {
        this.accountId = accountId;
        this.loginId = loginId;
        this.username = username;
        this.nickname = nickname;
        this.password = password;
        this.authorities = authorities;
        this.createdAt = LocalDateTime.now();
    }

    public Account update(Account account) {
        loginId = account.loginId;
        password = account.password;
        username = account.username;
        nickname = account.nickname;
        authorities = account.authorities;

        return this;
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
}
