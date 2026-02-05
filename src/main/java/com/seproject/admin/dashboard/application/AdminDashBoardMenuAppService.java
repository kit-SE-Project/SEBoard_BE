package com.seproject.admin.dashboard.application;

import com.seproject.account.account.domain.Account;
import com.seproject.account.role.domain.Role;
import com.seproject.account.role.service.RoleService;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.admin.dashboard.domain.DashBoardMenu;
import com.seproject.admin.dashboard.service.AdminDashBoardServiceImpl;
import com.seproject.admin.menu.domain.SelectOption;
import com.seproject.admin.menu.controller.dto.MenuDTO;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomAccessDeniedException;
import com.seproject.error.exception.CustomAuthenticationException;
import com.seproject.error.exception.CustomIllegalArgumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.seproject.admin.dashboard.controller.dto.DashBoardDTO.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashBoardMenuAppService {

    private final AdminDashBoardServiceImpl adminDashBoardServiceImpl;
    private final RoleService roleService;

    public DashBoardMenuResponse findDashBoardMenus() {
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN, null));

        List<Long> ids = adminDashBoardServiceImpl.findAuthorizeDashBoardMenuIds(account);
        List<DashBoardMenu> dashBoardMenu = adminDashBoardServiceImpl.findDashBoardMenu(ids);

        DashBoardMenuResponse response = DashBoardMenuResponse.toDTO(dashBoardMenu);

        return response;
    }

    public DashBoardMenuAuthorizationResponse findDashBoardMenuOptions() {
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN, null));

        boolean isAdmin = account.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(Role.ROLE_ADMIN));

        if(!isAdmin){
            throw new CustomAccessDeniedException(ErrorCode.ACCESS_DENIED, null);
        }

        List<Long> ids = adminDashBoardServiceImpl.findAuthorizeDashBoardMenuIds(account);
        List<DashBoardMenu> dashBoardMenus = adminDashBoardServiceImpl.findDashBoardMenuWithRole(ids);

        DashBoardMenuAuthorizationResponse response = DashBoardMenuAuthorizationResponse.toDTO(dashBoardMenus);
        return response;
    }

    @Transactional
    public void update(DashBoardUpdateRequest request) {
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN, null));

        boolean isAdmin = account.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(Role.ROLE_ADMIN));

        if(!isAdmin){
            throw new CustomAccessDeniedException(ErrorCode.ACCESS_DENIED, null);
        }

        List<DashBoardUpdateRequestElement> menus = request.getMenus();

        List<Long> ids = menus.stream()
                .map(DashBoardUpdateRequestElement::getId)
                .collect(Collectors.toList());

        List<DashBoardMenu> dashBoardMenuWithRole = adminDashBoardServiceImpl.findDashBoardMenuWithRole(ids);

        if(dashBoardMenuWithRole.size() != ids.size()) {
            throw new CustomIllegalArgumentException(ErrorCode.NOT_EXIST_DASHBOARDMENU, null);
        }
        Map<Long, DashBoardMenu> collect = dashBoardMenuWithRole.stream()
                .collect(Collectors.toMap(DashBoardMenu::getId, Function.identity()));


        for (DashBoardUpdateRequestElement menu : menus) {
            Long id = menu.getId();
            MenuDTO.MenuAuthOption option = menu.getOption();

            DashBoardMenu dashBoardMenu = collect.get(id);

            SelectOption selectOption = SelectOption.of(option.getOption());
            List<Role> roles = parseRoles(option);
            adminDashBoardServiceImpl.update(selectOption, dashBoardMenu,roles);
        }


    }

    protected List<Role> parseRoles(MenuDTO.MenuAuthOption option) {
        SelectOption selectOption = SelectOption.of(option.getOption());

        if(selectOption == SelectOption.SELECT) {
            List<Long> roleIds = option.getRoles();

            Role adminRole = roleService.findByName(Role.ROLE_ADMIN);
            List<Role> roleList = roleService.findByIds(roleIds);

            if(!roleList.contains(adminRole)){
                roleList.add(adminRole);
            }

            return roleList;
        }

        return roleService.convertRoles(selectOption);
    }




}
