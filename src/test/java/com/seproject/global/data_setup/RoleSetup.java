package com.seproject.global.data_setup;

import com.seproject.account.role.domain.Role;
import com.seproject.account.role.domain.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.UUID;

@Component
public class RoleSetup {

    @Autowired
    private RoleRepository roleRepository;


    @PostConstruct
    public void init() {
        Role roleUser = Role.builder()
                .roleId(1L).name(Role.ROLE_USER)
                .description("준회원 설명")
                .alias("준회원")
                .build();

        roleRepository.save(roleUser);

        Role roleKumoh = Role.builder()
                .roleId(2L).name(Role.ROLE_KUMOH)
                .description("금오인 설명")
                .alias("금오인")
                .build();

        roleRepository.save(roleKumoh);


        Role roleAdmin = Role.builder()
                .roleId(3L).name(Role.ROLE_ADMIN)
                .description("관리자 설명")
                .alias("관리자")
                .build();

        roleRepository.save(roleAdmin);
    }
    public Role getRoleUser(){
        return roleRepository.findByName(Role.ROLE_USER)
                .orElseThrow();
    }

    public Role getRoleKumoh(){
        return roleRepository.findByName(Role.ROLE_KUMOH)
                .orElseThrow();
    }

    public Role getRoleAdmin(){
        return roleRepository.findByName(Role.ROLE_ADMIN)
                .orElseThrow();
    }

    public Role createRole() {
        Role build = Role.builder()
                .name("ROLE_" + UUID.randomUUID().toString())
                .description(UUID.randomUUID().toString())
                .alias(UUID.randomUUID().toString())
                .build();

        roleRepository.save(build);

        return build;
    }
}
