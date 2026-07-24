package com.seproject.admin.menu.utils;

import com.seproject.account.authorization.domain.MenuAccessAuthorization;
import com.seproject.account.authorization.domain.MenuExposeAuthorization;
import com.seproject.account.authorization.service.AuthorizationService;
import com.seproject.account.role.domain.Role;
import com.seproject.account.role.service.RoleService;
import com.seproject.admin.menu.domain.SelectOption;
import com.seproject.admin.menu.controller.dto.MenuDTO;
import com.seproject.admin.menu.service.AdminMenuService;
import com.seproject.board.menu.domain.model.Menu;
import com.seproject.board.menu.service.MenuService;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;

import java.util.List;

public class MenuProvider extends AbstractMenuProvider{

    public MenuProvider(AdminMenuService adminMenuService, MenuService menuService, RoleService roleService, AuthorizationService authorizationService) {
        super(adminMenuService, menuService, roleService, authorizationService);
    }

    @Override
    protected boolean support(Menu menu) {
        return Menu.class.isAssignableFrom(menu.getClass());
    }

    @Override
    public Long create(MenuDTO.CreateMenuRequest request, String categoryType) {
        if (categoryType.equals("MENU")) {
            String name = request.getName();
            String description = request.getDescription();
            String urlInfo = request.getUrlId();

            Menu superMenu = null;
            Long superCategoryId = request.getSuperCategoryId();
            if (superCategoryId != null) {
                superMenu = menuService.findById(superCategoryId);
            }

            Long menuId = adminMenuService.createMenu(superMenu,name,description, urlInfo);

            MenuDTO.MenuAuthOption expose = request.getExpose();

            if(expose == null) {
                throw new CustomIllegalArgumentException(ErrorCode.INVALID_MENU_REQUEST, null);
            }

            Menu menu = menuService.findById(menuId);

            List<Role> exposeRoles = parseRoles(expose);
            SelectOption exposeSelectOption = SelectOption.of(expose.getOption());
            authorizationService.updateExpose(menu,exposeSelectOption,exposeRoles);
            return menuId;
        }
        return null;
    }

    @Override
    public Long update(Menu menu, MenuDTO.UpdateMenuRequest request) {
        if (support(menu)) {
            String description = request.getDescription();
            String name = request.getName();
            String urlId = request.getUrlId();

            if(!menu.getUrlInfo().equals(urlId)){
                adminMenuService.validUrlInfo(urlId);
            }

            menu.changeDescription(description);
            menu.changeName(name);
            menu.changeUrlInfo(urlId);

            MenuDTO.MenuAuthOption access = request.getAccess();
            MenuDTO.MenuAuthOption expose = request.getExpose();

            if (access == null || expose == null){
                throw new CustomIllegalArgumentException(ErrorCode.INVALID_MENU_REQUEST, null);
            }

            MenuAccessAuthorization menuAccessAuthorization = new MenuAccessAuthorization(menu);
            menuAccessAuthorization.update(parseRoles(access));

            List<Role> roles = parseRoles(expose);
            MenuExposeAuthorization menuExposeAuthorization = new MenuExposeAuthorization(menu);
            menuExposeAuthorization.update(roles);
            menu.updateMenuAuthorizations(List.of(menuAccessAuthorization, menuExposeAuthorization));

            return menu.getMenuId();
        }

        return null;
    }

    @Override
    public MenuDTO.MenuResponse toDto(Menu menu) {
        if (support(menu)) {

        }
        return null;
    }
}
