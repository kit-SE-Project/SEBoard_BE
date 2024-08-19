package com.seproject.account.authorization.domain;

import com.seproject.account.authorization.utils.AuthorizationProperty;
import com.seproject.account.role.domain.Role;
import com.seproject.account.role.domain.RoleAuthorization;
import com.seproject.admin.menu.domain.SelectOption;
import com.seproject.board.menu.domain.model.Menu;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@Entity
public abstract class MenuAuthorization extends Authorization {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id")
    protected Menu menu;

    public MenuAuthorization(Menu menu) {
        this.menu = menu;
    }

    public boolean support(AuthorizationProperty property){
        return property.equals(getAuthorizationProperty());
    }
    public boolean isAuth(List<Role> roles) {

        if (roleAuthorizations.size() == 0) return true;

        for (RoleAuthorization roleAuthorization : roleAuthorizations) {
            for (Role role : roles) {
                Role roleAuthorizationRole = roleAuthorization.getRole();
                if(role.getId().equals(roleAuthorizationRole.getId())) {
                    return true;
                }
            }
        }

        return false;
    }

    public void setMenu(Menu menu) {
        this.menu = menu;
    }
    public abstract void setSelectOption(SelectOption selectOption);
    public abstract SelectOption getSelectOption();
    public abstract AuthorizationProperty getAuthorizationProperty();

    public List<Role> getAvailableRoles() {
        List<Role> roles = new ArrayList<>();

        for (RoleAuthorization roleAuthorization : roleAuthorizations) {
            roles.add(roleAuthorization.getRole());
        }

        return roles;
    }
}
