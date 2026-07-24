package com.seproject.admin.menu.utils;

import com.seproject.account.authorization.domain.MenuAccessAuthorization;
import com.seproject.account.authorization.domain.MenuEditAuthorization;
import com.seproject.account.authorization.domain.MenuExposeAuthorization;
import com.seproject.account.authorization.domain.MenuManageAuthorization;
import com.seproject.account.authorization.service.AuthorizationService;
import com.seproject.account.role.domain.Role;
import com.seproject.account.role.service.RoleService;
import com.seproject.admin.menu.domain.SelectOption;
import com.seproject.admin.menu.controller.dto.MenuDTO;
import com.seproject.admin.menu.service.AdminMenuService;
import com.seproject.board.menu.domain.model.Menu;
import com.seproject.board.menu.domain.model.RecruitMenu;
import com.seproject.board.menu.service.MenuService;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;

import java.util.List;

public class RecruitMenuProvider extends AbstractMenuProvider {

    public RecruitMenuProvider(AdminMenuService adminMenuService, MenuService menuService,
                               RoleService roleService, AuthorizationService authorizationService) {
        super(adminMenuService, menuService, roleService, authorizationService);
    }

    @Override
    protected boolean support(Menu menu) {
        return menu instanceof RecruitMenu;
    }

    @Override
    public Long create(MenuDTO.CreateMenuRequest request, String categoryType) {
        return null;
    }

    @Override
    public Long update(Menu menu, MenuDTO.UpdateMenuRequest request) {
        if (!support(menu)) return null;

        String name = request.getName();
        menu.changeName(name);

        MenuDTO.MenuAuthOption access  = request.getAccess();
        MenuDTO.MenuAuthOption expose  = request.getExpose();
        MenuDTO.MenuAuthOption write   = request.getWrite();
        MenuDTO.MenuAuthOption manage  = request.getManage();

        if (access == null || expose == null || write == null || manage == null)
            throw new CustomIllegalArgumentException(ErrorCode.INVALID_MENU_REQUEST, null);

        if ("ALL".equalsIgnoreCase(write.getOption()))
            throw new CustomIllegalArgumentException(ErrorCode.INVALID_MENU_REQUEST, null);
        if ("ALL".equalsIgnoreCase(manage.getOption()))
            throw new CustomIllegalArgumentException(ErrorCode.INVALID_MENU_REQUEST, null);

        List<Role> accessRoles  = parseRoles(access);
        SelectOption accessOpt  = SelectOption.of(access.getOption());
        MenuAccessAuthorization accessAuth = new MenuAccessAuthorization(menu);
        accessAuth.update(accessRoles);
        accessAuth.setSelectOption(accessOpt);

        List<Role> exposeRoles  = parseRoles(expose);
        SelectOption exposeOpt  = SelectOption.of(expose.getOption());
        MenuExposeAuthorization exposeAuth = new MenuExposeAuthorization(menu);
        exposeAuth.update(exposeRoles);
        exposeAuth.setSelectOption(exposeOpt);

        List<Role> writeRoles   = parseRoles(write);
        SelectOption writeOpt   = SelectOption.of(write.getOption());
        MenuEditAuthorization writeAuth = new MenuEditAuthorization(menu);
        writeAuth.update(writeRoles);
        writeAuth.setSelectOption(writeOpt);

        List<Role> manageRoles  = parseRoles(manage);
        SelectOption manageOpt  = SelectOption.of(manage.getOption());
        MenuManageAuthorization manageAuth = new MenuManageAuthorization(menu);
        manageAuth.update(manageRoles);
        manageAuth.setSelectOption(manageOpt);

        menu.updateMenuAuthorizations(List.of(accessAuth, exposeAuth, writeAuth, manageAuth));
        return menu.getMenuId();
    }

    @Override
    public MenuDTO.MenuResponse toDto(Menu menu) {
        return null;
    }
}
