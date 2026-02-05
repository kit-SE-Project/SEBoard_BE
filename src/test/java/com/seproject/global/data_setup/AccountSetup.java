package com.seproject.global.data_setup;

import com.seproject.account.account.domain.Account;
import com.seproject.account.account.domain.FormAccount;
import com.seproject.account.account.domain.OAuthAccount;
import com.seproject.account.account.domain.repository.AccountRepository;
import com.seproject.account.account.domain.repository.OAuthAccountRepository;
import com.seproject.account.role.domain.Role;
import com.seproject.account.role.domain.RoleAccount;
import com.seproject.account.token.domain.JWT;
import com.seproject.account.token.service.TokenService;
import com.seproject.board.common.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AccountSetup {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OAuthAccountRepository oAuthAccountRepository;

    @Autowired
    private RoleSetup roleSetup;

    private Account adminAccount;

    @Autowired TokenService tokenService;

    @PostConstruct
    public void init() {

        adminAccount = createFormAccount("admin",
                "admin",
                List.of(roleSetup.getRoleAdmin(), roleSetup.getRoleKumoh(), roleSetup.getRoleUser()),
                LocalDateTime.now(), Status.NORMAL);

        accountRepository.save(adminAccount);

        UsernamePasswordAuthenticationToken token
                = new UsernamePasswordAuthenticationToken(adminAccount.getLoginId(),adminAccount.getPassword(),adminAccount.getAuthorities());

        JWT accessToken = tokenService.createAccessToken(token);
    }

    public Account getAdminAccount() {
        return adminAccount;
    }


    public FormAccount createFormAccount(String loginId,
                                         String name,
                                         List<Role> roles,
                                         LocalDateTime createdAt,
                                         Status status) {

        List<RoleAccount> roleAccounts = roles.stream().map((role) -> new RoleAccount(null, role))
                .collect(Collectors.toList());

        FormAccount formAccount = FormAccount.builder()
                .loginId(loginId)
                .name(name)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .roleAccounts(roleAccounts)
                .createdAt(createdAt)
                .status(status)
                .build();
        for (RoleAccount roleAccount : roleAccounts) {
            roleAccount.setAccount(formAccount);
        }
        accountRepository.save(formAccount);
        return formAccount;
    }

    public FormAccount createRandomUserAccount(){
        return createFormAccount(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                List.of(roleSetup.getRoleUser()), LocalDateTime.now(), Status.NORMAL);
    }

    public FormAccount createFormAccount() {
        Role roleUser = roleSetup.getRoleUser();
        RoleAccount roleAccount = new RoleAccount(null,roleUser);
        FormAccount formAccount = FormAccount.builder()
                .loginId(UUID.randomUUID().toString())
                .name("se")
                .password(passwordEncoder.encode("1234"))
                .roleAccounts(new ArrayList<>(List.of(roleAccount)))
                .createdAt(LocalDateTime.now())
                .status(Status.NORMAL)
                .build();
        roleAccount.setAccount(formAccount);
        accountRepository.save(formAccount);
        return formAccount;
    }

    public OAuthAccount createOAuthAccount() {
        Role roleUser = roleSetup.getRoleUser();
        RoleAccount roleAccount = new RoleAccount(null,roleUser);
        OAuthAccount oAuthAccount = OAuthAccount.builder()
                .loginId(UUID.randomUUID().toString())
                .name("se_oauth")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .roleAccounts(new ArrayList<>(List.of(roleAccount)))
                .provider("kakao")
                .sub("123123")
                .createdAt(LocalDateTime.now())
                .status(Status.NORMAL)
                .build();
        roleAccount.setAccount(oAuthAccount);
        oAuthAccountRepository.save(oAuthAccount);
        return oAuthAccount;
    }
}
