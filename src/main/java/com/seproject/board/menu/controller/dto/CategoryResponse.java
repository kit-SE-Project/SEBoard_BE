package com.seproject.board.menu.controller.dto;


import com.seproject.account.role.domain.Role;
import com.seproject.board.menu.domain.model.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class CategoryResponse {
    private Long menuId;
    private String name;
    private String urlId;
    private String externalUrl;
    private String type;
    private List<CategoryResponse> subMenu = new ArrayList<>();
    private Boolean popularPostEnabled;
    private boolean accessible;
    private boolean manageable;

    public CategoryResponse(Menu menu) {
        this(menu, List.of());
    }

    public CategoryResponse(Menu menu, List<Role> roles) {
        this.menuId = menu.getMenuId();
        this.name = menu.getName();
        this.accessible = menu.accessible(roles);
        this.manageable = menu.manageable(roles);

        if (menu.getClass() == ExternalSiteMenu.class) {
            this.externalUrl = menu.getUrlInfo();
        } else {
            this.urlId = menu.getUrlInfo();
        }

        if (menu.getClass() == Menu.class) {
            this.type = "MENU";
        } else if (menu.getClass() == BoardMenu.class) {
            this.type = "BOARD";
        } else if (menu.getClass() == Category.class) {
            this.type = "CATEGORY";
            this.popularPostEnabled = ((Category) menu).isPopularPostEnabled();
        } else if (menu.getClass() == ExternalSiteMenu.class) {
            this.type = "EXTERNAL";
        } else if (menu.getClass() == RecruitMenu.class) {
            this.type = "RECRUIT";
        }
    }

    public void addSubMenu(CategoryResponse subMenu) {
        this.subMenu.add(subMenu);
    }
}
