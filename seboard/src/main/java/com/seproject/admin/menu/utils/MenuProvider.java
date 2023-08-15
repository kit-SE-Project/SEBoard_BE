package com.seproject.admin.menu.utils;

import com.seproject.account.authorization.domain.MenuAccessAuthorization;
import com.seproject.account.authorization.domain.MenuAuthorization;
import com.seproject.account.authorization.domain.MenuExposeAuthorization;
import com.seproject.account.authorization.service.AuthorizationService;
import com.seproject.account.role.domain.Role;
import com.seproject.account.role.service.RoleService;
import com.seproject.admin.domain.SelectOption;
import com.seproject.admin.menu.controller.dto.MenuDTO;
import com.seproject.admin.menu.service.MenuService;
import com.seproject.board.menu.domain.Menu;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;

import java.util.List;
import java.util.stream.Collectors;

public class MenuProvider extends AbstractMenuProvider{

    public MenuProvider(MenuService menuService, RoleService roleService, AuthorizationService authorizationService) {
        super(menuService, roleService, authorizationService);
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
            if (request.getSuperCategoryId() != null) {
                superMenu = menuService.findById(request.getSuperCategoryId());
            }

            Long menuId = menuService.createMenu(superMenu,name,description, urlInfo);

            MenuDTO.MenuAuthOption access = request.getAccess();
            MenuDTO.MenuAuthOption expose = request.getExpose();

            if(access == null || expose == null) {
                throw new CustomIllegalArgumentException(ErrorCode.INVALID_MENU_REQUEST, null);
            }

            Menu menu = menuService.findById(menuId);

            SelectOption selectOption = SelectOption.of(access.getName());
            List<Role> roles = roleService.convertRoles(selectOption);
            authorizationService.updateAccess(menu,selectOption,roles);

            SelectOption exposeSelectOption = SelectOption.of(expose.getName());
            List<Role> exposeRoles = roleService.convertRoles(exposeSelectOption);
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
