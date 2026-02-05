package com.seproject.admin.role.application;

import com.seproject.account.account.domain.Account;
import com.seproject.account.role.domain.Role;
import com.seproject.account.role.service.RoleService;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.admin.dashboard.domain.DashBoardMenu;
import com.seproject.admin.dashboard.service.AdminDashBoardServiceImpl;
import com.seproject.admin.role.controller.dto.RoleDTO.CreateRoleRequest;
import com.seproject.admin.role.controller.dto.RoleDTO.RoleResponse;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomAccessDeniedException;
import com.seproject.error.exception.CustomAuthenticationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.seproject.admin.role.controller.dto.RoleDTO.UpdateRoleRequest;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class AdminRoleAppService {

    private final RoleService roleService;

    private final AdminDashBoardServiceImpl dashBoardService;

    //TODO : 추후 AOP로 변경 필요
    private void checkAuthorization(List<String> urls){
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN, null));

        boolean hasAuthorization = true;

        for(String url : urls){
            DashBoardMenu dashBoardMenu = dashBoardService.findDashBoardMenuByUrl(url);
            if(!dashBoardMenu.authorize(account.getRoles())){
                hasAuthorization = false;
            }
        }

        if(!hasAuthorization){
            throw new CustomAccessDeniedException(ErrorCode.ACCESS_DENIED, null);
        }
    }

    public List<RoleResponse> findAll(){
        checkAuthorization(List.of(DashBoardMenu.ROLE_MANAGE_URL, DashBoardMenu.ACCOUNT_MANAGE_URL));

        List<Role> roles = roleService.findAll();

        return roles.stream()
                .map(RoleResponse::of)
                .collect(Collectors.toList());
    }

    @Transactional
    public Long createRole(CreateRoleRequest request) {
        checkAuthorization(List.of(DashBoardMenu.ROLE_MANAGE_URL));

        return roleService.createRole(request.getName(), request.getDescription(), request.getAlias());
    }

    @Transactional
    public void deleteRole(Long roleId) {
        checkAuthorization(List.of(DashBoardMenu.ROLE_MANAGE_URL));

        roleService.deleteRole(roleId);
    }

    @Transactional
    public void updateRole(Long roleId, UpdateRoleRequest request) {
        checkAuthorization(List.of(DashBoardMenu.ROLE_MANAGE_URL));

        //TODO
        roleService.updateRole(roleId,request.getName(),request.getDescription(),request.getAlias());
    }


}
