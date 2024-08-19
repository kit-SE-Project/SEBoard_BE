package com.seproject.admin.menu.utils;

import com.seproject.account.authorization.domain.MenuAuthorization;
import com.seproject.account.authorization.domain.MenuEditAuthorization;
import com.seproject.account.authorization.domain.MenuManageAuthorization;
import com.seproject.account.authorization.service.AuthorizationService;
import com.seproject.account.role.domain.Role;
import com.seproject.account.role.service.RoleService;
import com.seproject.admin.menu.domain.SelectOption;
import com.seproject.admin.menu.controller.dto.MenuDTO;
import com.seproject.admin.menu.service.AdminMenuService;
import com.seproject.board.menu.domain.model.Category;
import com.seproject.board.menu.domain.model.Menu;
import com.seproject.board.menu.service.MenuService;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;

import java.util.List;
import java.util.stream.Collectors;

public class CategoryProvider extends AbstractMenuProvider {

    public CategoryProvider(AdminMenuService adminMenuService, MenuService menuService, RoleService roleService, AuthorizationService authorizationService) {
        super(adminMenuService, menuService, roleService, authorizationService);
    }

    @Override
    protected boolean support(Menu menu) {
        return Category.class.isAssignableFrom(menu.getClass());
    }

    @Override
    public Long create(MenuDTO.CreateMenuRequest request, String categoryType) {

        if (categoryType.equals("CATEGORY")) {
            Long superCategoryId = request.getSuperCategoryId();
            String name = request.getName();
            String description = request.getDescription();
            String urlInfo = request.getUrlId();

            if(superCategoryId == null) {
                throw new CustomIllegalArgumentException(ErrorCode.NOT_EXIST_MENU,null);
            }

            Menu superMenu = menuService.findById(superCategoryId);
            Long categoryId = adminMenuService.createCategory(superMenu, name, description, urlInfo);

            MenuDTO.MenuAuthOption manage = request.getManage();
            MenuDTO.MenuAuthOption edit = request.getWrite();

            if(manage == null || edit == null) {
                throw new CustomIllegalArgumentException(ErrorCode.INVALID_MENU_REQUEST, null);
            }

            Menu menu = menuService.findById(categoryId);

            if(manage.getOption().equals("ALL")){
                throw new CustomIllegalArgumentException(ErrorCode.INVALID_REQUEST);
            }
            
            List<Role> manageRoles = parseRoles(manage);
            SelectOption manageSelectOption = SelectOption.of(manage.getOption());
            authorizationService.updateManage(menu,manageSelectOption,manageRoles);

            List<Role> editRoles = parseRoles(edit);
            SelectOption editSelectOption = SelectOption.of(edit.getOption());
            authorizationService.updateEdit(menu,editSelectOption,editRoles);

            return categoryId;
        }

        return null;
    }

    @Override
    public Long update(Menu menu, MenuDTO.UpdateMenuRequest request) {
        if(support(menu)) {
            String description = request.getDescription();
            String name = request.getName();
            String urlId = request.getUrlId();

            if(!menu.getUrlInfo().equals(urlId)){
                adminMenuService.validUrlInfo(urlId);
            }

            menu.changeDescription(description);
            menu.changeName(name);
            menu.changeUrlInfo(urlId);

            MenuDTO.MenuAuthOption edit = request.getWrite();
            MenuDTO.MenuAuthOption manage = request.getManage();

            if (edit == null || manage == null)
                throw new CustomIllegalArgumentException(ErrorCode.INVALID_MENU_REQUEST, null);

            List<Role> editRoles = parseRoles(edit);
            SelectOption editSelectOption = SelectOption.of(edit.getOption());
            MenuEditAuthorization menuEditAuthorization = new MenuEditAuthorization(menu);
            menuEditAuthorization.update(editRoles);
            menuEditAuthorization.setSelectOption(editSelectOption);

            SelectOption manageSelectOption = SelectOption.of(manage.getOption());
            List<Role> manageRoles = parseRoles(manage);
            MenuManageAuthorization menuManageAuthorization = new MenuManageAuthorization(menu);
            menuManageAuthorization.update(manageRoles);
            menuManageAuthorization.setSelectOption(manageSelectOption);

            menu.updateMenuAuthorizations(List.of(menuEditAuthorization,menuManageAuthorization));
            return menu.getMenuId();
        }

        return null;
    }

    @Override
    public MenuDTO.MenuResponse toDto(Menu menu) {
        if(support(menu)) {
            List<Menu> subMenu = menuService.findSubMenu(menu.getMenuId());
            List<MenuDTO.MenuResponse> menuResponses = subMenu.stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());

            MenuDTO.MenuResponse menuResponse = new MenuDTO.MenuResponse(menu,menuResponses);

            List<MenuAuthorization> menuAuthorizations = menu.getMenuAuthorizations();
            for (int i = 0; i < menuAuthorizations.size(); i++) {
                //TODO : 채우기

            }



            return menuResponse;
        }

        return null;
    }
}
