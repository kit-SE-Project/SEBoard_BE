package com.seproject.account.authorization.service;

import com.seproject.account.authorization.domain.*;
import com.seproject.account.role.domain.Role;
import com.seproject.account.authorization.domain.repository.AuthorizationRepository;
import com.seproject.admin.menu.domain.SelectOption;
import com.seproject.board.menu.domain.model.Menu;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class AuthorizationService {

    private final AuthorizationRepository authorizationRepository;

    public MenuAccessAuthorization findAccessAuthorization(Long menuId) {
        return authorizationRepository.findMenuAccessAuthorization(menuId).orElseThrow();
    }

    public MenuEditAuthorization findEditAuthorization(Long menuId) {
        return authorizationRepository.findMenuEditAuthorization(menuId).orElseThrow();
    }

    public MenuManageAuthorization findManageAuthorization(Long menuId) {
        return authorizationRepository.findMenuManageAuthorization(menuId).orElseThrow();
    }

    public MenuExposeAuthorization findExposeAuthorization(Long menuId) {
        return authorizationRepository.findMenuExposeAuthorization(menuId).orElseThrow();
    }

    @Transactional
    public void updateAccess(Menu menu, SelectOption selectOption,List<Role> roles) {
        Optional<MenuAccessAuthorization> accessAuthorizationOption = authorizationRepository.findMenuAccessAuthorization(menu.getMenuId());
        MenuAccessAuthorization menuAccessAuthorization;

        if (accessAuthorizationOption.isEmpty()) {
            menuAccessAuthorization = new MenuAccessAuthorization(menu);
            menu.addAuthorization(menuAccessAuthorization);
        } else {
            menuAccessAuthorization = accessAuthorizationOption.get();
        }

        menuAccessAuthorization.setSelectOption(selectOption);
        menuAccessAuthorization.update(roles);
    }

    @Transactional
    public void updateEdit(Menu menu, SelectOption selectOption,List<Role> roles) {
        Optional<MenuEditAuthorization> optional = authorizationRepository.findMenuEditAuthorization(menu.getMenuId());
        MenuEditAuthorization menuEditAuthorization;

        if (optional.isEmpty()) {
            menuEditAuthorization = new MenuEditAuthorization(menu);
            menu.addAuthorization(menuEditAuthorization);
        } else {
            menuEditAuthorization = optional.get();
        }

        menuEditAuthorization.update(roles);
        menuEditAuthorization.setSelectOption(selectOption);
    }

    @Transactional
    public void updateExpose(Menu menu, SelectOption selectOption,List<Role> roles) {
        Optional<MenuExposeAuthorization> optional = authorizationRepository.findMenuExposeAuthorization(menu.getMenuId());
        MenuExposeAuthorization menuExposeAuthorization;

        if (optional.isEmpty()) {
            menuExposeAuthorization = new MenuExposeAuthorization(menu);
            menu.addAuthorization(menuExposeAuthorization);
        } else {
            menuExposeAuthorization = optional.get();
        }

        menuExposeAuthorization.update(roles);
        menuExposeAuthorization.setSelectOption(selectOption);
    }

    @Transactional
    public void updateManage(Menu menu, SelectOption selectOption,List<Role> roles) {
        Optional<MenuManageAuthorization> optional = authorizationRepository.findMenuManageAuthorization(menu.getMenuId());
        MenuManageAuthorization menuManageAuthorization;

        if (optional.isEmpty()) {
            menuManageAuthorization = new MenuManageAuthorization(menu);
            menu.addAuthorization(menuManageAuthorization);
        } else {
            menuManageAuthorization = optional.get();
        }

        menuManageAuthorization.update(roles);
        menuManageAuthorization.setSelectOption(selectOption);
    }



}
