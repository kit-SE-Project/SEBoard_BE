package com.seproject.account.role.service;

import com.seproject.account.role.domain.Role;
import com.seproject.account.role.domain.repository.RoleRepository;
import com.seproject.admin.menu.domain.SelectOption;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public List<Role> findByIds(List<Long> ids) {
        List<Role> allById = roleRepository.findAllById(ids);
        if (ids.size() != allById.size()) throw new CustomIllegalArgumentException(ErrorCode.ROLE_NOT_FOUND,null);
        return allById;
    }

    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    public Role findById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new CustomIllegalArgumentException(ErrorCode.ROLE_NOT_FOUND,null));
    }

    public Role findByName(String name) {

        if (!name.startsWith("ROLE_")) {
            name = "ROLE_" + name;
        }

        return roleRepository.findByName(name)
                .orElseThrow(() -> new CustomIllegalArgumentException(ErrorCode.ROLE_NOT_FOUND,null));
    }

    public List<Role> findByNameIn(List<String> roleNames) {
        return roleRepository.findByNameIn(roleNames);
    }

    @Transactional
    public Long createRole(String roleName,String description,String alias) {

        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName;
        }

        if (Role.isImmutable(roleName))
            throw new CustomIllegalArgumentException(ErrorCode.IMMUTABLE_ROLE,null);

        Role role = Role.builder()
                .name(roleName)
                .description(description)
                .alias(alias)
                .build();

        roleRepository.save(role);
        return role.getId();
    }

    @Transactional
    public void deleteRole(Long roleId) {

        Role role = roleRepository.findById(roleId).orElseThrow(
                () -> new CustomIllegalArgumentException(ErrorCode.ROLE_NOT_FOUND,null));

        if(role.isImmutable()) {
            throw new CustomIllegalArgumentException(ErrorCode.IMMUTABLE_ROLE,null);
        }

        roleRepository.delete(role);
    }

    @Transactional
    public Long updateRole(Long roleId,String name, String description, String alias) {

        Role role = roleRepository.findById(roleId).orElseThrow(() ->
            new CustomIllegalArgumentException(ErrorCode.ROLE_NOT_FOUND,null));

        if (!name.startsWith("ROLE_")) {
            name = "ROLE_" + name;
        }

        if(role.isImmutable()) {
            throw new CustomIllegalArgumentException(ErrorCode.IMMUTABLE_ROLE,null);
        }

        role.update(name,description,alias);
        return role.getId();
    }


    public List<Role> convertRoles(SelectOption selectOption) {
        List<String> roleNames;

        if (selectOption == null) {
            throw new CustomIllegalArgumentException(ErrorCode.SELECT_OPTION_NOT_FOUND,null);
        }

        switch (selectOption){
            case OVER_USER: {
                roleNames = List.of(Role.ROLE_USER,Role.ROLE_KUMOH,Role.ROLE_ADMIN);
                return roleRepository.findByNameIn(roleNames);
            }
            case OVER_KUMOH: {
                roleNames = List.of(Role.ROLE_KUMOH,Role.ROLE_ADMIN);
                return roleRepository.findByNameIn(roleNames);
            }
            case ONLY_ADMIN: {
                roleNames = List.of(Role.ROLE_ADMIN);
                return roleRepository.findByNameIn(roleNames);
            }
            case ALL: {
                return List.of();
            }
            case SELECT: {
                return new ArrayList<>();
            }
        }

        throw new CustomIllegalArgumentException(ErrorCode.SELECT_OPTION_NOT_FOUND,null);
    }

}
