package com.seproject.admin.menu.utils;

import com.seproject.account.authorization.service.AuthorizationService;
import com.seproject.account.role.domain.Role;
import com.seproject.account.role.service.RoleService;
import com.seproject.admin.domain.SelectOption;
import com.seproject.admin.menu.service.MenuService;
import com.seproject.board.menu.domain.Menu;

import java.util.List;

import static com.seproject.admin.menu.controller.dto.MenuDTO.*;

public abstract class AbstractMenuProvider {

     protected final MenuService menuService;
     protected final RoleService roleService;
     protected final AuthorizationService authorizationService;

     public AbstractMenuProvider(MenuService menuService, RoleService roleService, AuthorizationService authorizationService) {
          this.menuService = menuService;
          this.roleService = roleService;
          this.authorizationService = authorizationService;
     }

     protected abstract boolean support(Menu menu);

     public abstract Long create(CreateMenuRequest request, String categoryType);
     public abstract Long update(Menu menu, UpdateMenuRequest request);

     public abstract MenuResponse toDto(Menu menu);

     protected List<Role> parseRoles(MenuAuthOption option) {
          SelectOption selectOption = SelectOption.of(option.getName());
          if(selectOption == SelectOption.SELECT) {
               List<Long> roleIds = option.getRoles();
               return roleService.findByIds(roleIds);
          }

          return roleService.convertRoles(selectOption);
     }
}
