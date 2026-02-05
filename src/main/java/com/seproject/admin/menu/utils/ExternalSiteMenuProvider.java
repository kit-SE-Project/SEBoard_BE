package com.seproject.admin.menu.utils;

import com.seproject.account.authorization.domain.MenuExposeAuthorization;
import com.seproject.account.authorization.service.AuthorizationService;
import com.seproject.account.role.domain.Role;
import com.seproject.account.role.service.RoleService;
import com.seproject.admin.menu.domain.SelectOption;
import com.seproject.admin.menu.controller.dto.MenuDTO;
import com.seproject.admin.menu.service.AdminMenuService;
import com.seproject.board.menu.domain.model.ExternalSiteMenu;
import com.seproject.board.menu.domain.model.Menu;
import com.seproject.board.menu.service.MenuService;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;

import java.util.List;

public class ExternalSiteMenuProvider extends AbstractMenuProvider {

    public ExternalSiteMenuProvider(AdminMenuService adminMenuService, MenuService menuService, RoleService roleService, AuthorizationService authorizationService) {
        super(adminMenuService, menuService, roleService, authorizationService);
    }

    @Override
    protected boolean support(Menu menu) {
        return ExternalSiteMenu.class.isAssignableFrom(menu.getClass());
    }

    @Override
    public Long create(MenuDTO.CreateMenuRequest request, String categoryType) {
        if (categoryType.equals("EXTERNAL")) {
            String name = request.getName();
            String description = request.getDescription();
            String urlInfo = request.getExternalUrl();

            Menu superMenu = null;
            if (request.getSuperCategoryId() != null) {
                superMenu = menuService.findById(request.getSuperCategoryId());
            }

            Long externalSiteMenuId = adminMenuService.createExternalSiteMenu(superMenu,name,description, urlInfo);

            MenuDTO.MenuAuthOption expose = request.getExpose();

            if(expose == null) {
                throw new CustomIllegalArgumentException(ErrorCode.INVALID_MENU_REQUEST,null);
            }

            Menu menu = menuService.findById(externalSiteMenuId);

            List<Role> exposeRoles = parseRoles(expose);
            SelectOption exposeSelectOption = SelectOption.of(expose.getOption());
            authorizationService.updateExpose(menu,exposeSelectOption,exposeRoles);
            return externalSiteMenuId;
        }
        return null;
    }

    @Override
    public Long update(Menu menu, MenuDTO.UpdateMenuRequest request) {
        if (support(menu)) {
            String description = request.getDescription();
            String name = request.getName();
            String externalUrl = request.getExternalUrl();

            Menu superMenu = null;
            if(request.getSuperMenuId() != null) {
                if(menu.getDepth() <= 1){
                    superMenu = menuService.findById(request.getSuperMenuId());
                }else{
                    throw new CustomIllegalArgumentException(ErrorCode.INVALID_REQUEST);
                }
            }

            menu.changeSuperMenu(superMenu);
            menu.changeDescription(description);
            menu.changeName(name);
            menu.changeUrlInfo(externalUrl);

            MenuDTO.MenuAuthOption expose = request.getExpose();

            if (expose == null)
                throw new CustomIllegalArgumentException(ErrorCode.INVALID_MENU_REQUEST, null);

            List<Role> exposeRoles = parseRoles(expose);
            SelectOption selectOption = SelectOption.of(expose.getOption());
            MenuExposeAuthorization menuExposeAuthorization = new MenuExposeAuthorization(menu);
            menuExposeAuthorization.update(exposeRoles);
            menuExposeAuthorization.setSelectOption(selectOption);

            menu.updateMenuAuthorizations(List.of(menuExposeAuthorization));
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
