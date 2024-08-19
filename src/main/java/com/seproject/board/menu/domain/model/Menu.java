package com.seproject.board.menu.domain.model;

import com.seproject.account.authorization.utils.AuthorizationProperty;
import com.seproject.account.role.domain.Role;
import com.seproject.account.authorization.domain.MenuAuthorization;
import com.seproject.admin.menu.domain.SelectOption;
import com.seproject.board.menu.service.MenuService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.util.Pair;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@Entity
@Getter
@Table(name = "menus")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "menu_type")
@DiscriminatorValue("MENU")
public class Menu {
    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 20;

    @Id @GeneratedValue
    private Long menuId;
    @ManyToOne
    @JoinColumn(name = "super_menu_id")
    protected Menu superMenu;
    private String name;
    private String description;
    private int depth;

    //TODO : Internal -> urlId, External -> urlInfo
    protected String urlInfo;

    @OneToMany(mappedBy = "menu",fetch = FetchType.LAZY, cascade = CascadeType.ALL,orphanRemoval = true)
    private List<MenuAuthorization> menuAuthorizations = new ArrayList<>();

    public Menu(Long menuId, Menu superMenu, String name, String description) {
        if(!isValidName(name)){
            throw new IllegalArgumentException();
        }

        this.menuId = menuId;
        this.superMenu = superMenu;
        this.name = name;
        this.description = description;
        this.depth = calculateDepth();
        this.menuAuthorizations = new ArrayList<>();
    }


    public String getType(){
        return "MENU";
    }

    public void addAuthorization(MenuAuthorization menuAuthorization) {
        this.menuAuthorizations.add(menuAuthorization);
        menuAuthorization.setMenu(this);
    }

    protected int calculateDepth() {
        if(superMenu == null){
            return 0;
        }else{
            return superMenu.getDepth() + 1;
        }
    }

    public boolean isRemovable(MenuService categoryService){
        return !categoryService.hasSubCategory(menuId);
    }

    public void changeName(String name) {
        if(!isValidName(name)){
            throw new IllegalArgumentException();
        }

        this.name = name;
    }

    private boolean isValidName(String name) {
        return MIN_NAME_LENGTH <= name.length() && name.length() <= MAX_NAME_LENGTH;
    }

    public void updateMenuAuthorizations(List<MenuAuthorization> menuAuthorizations) {
        this.menuAuthorizations.clear();
        this.menuAuthorizations.addAll(menuAuthorizations);
    }

    public void changeDescription(String description) {
        this.description = description;
    }

    public void changeSuperMenu(Menu superMenu) {
        this.superMenu = superMenu;
        this.depth = calculateDepth();
    }

    public void changeUrlInfo(String urlInfo) {this.urlInfo = urlInfo;}

    public Map<AuthorizationProperty, Pair<SelectOption, List<Role>>> getAvailableRoles() {
        Map<AuthorizationProperty, Pair<SelectOption, List<Role>>> res = new HashMap<>();

        for (MenuAuthorization menuAuthorization : menuAuthorizations) {
            AuthorizationProperty authProperty = menuAuthorization.getAuthorizationProperty();
            List<Role> availableRoles = menuAuthorization.getAvailableRoles();

            //TODO : 해당 MENU TYPE에 필요없는 Authorization이면 아예 Entity생성이 안되어야하는거 아닌가?
            if(menuAuthorization.getSelectOption()!=null){
                res.putIfAbsent(authProperty, Pair.of(menuAuthorization.getSelectOption(), new ArrayList<>()));
                res.get(authProperty).getSecond().addAll(availableRoles);
            }
        }

        return res;
    }

    //TODO : throw vs return true
    public boolean editable(List<Role> roles) {
        if (superMenu != null && !superMenu.editable(roles)) {
            return false;
        }

        for (MenuAuthorization menuAuthorization : menuAuthorizations) {
            if(menuAuthorization.support(AuthorizationProperty.EDITABLE)) {
                return menuAuthorization.isAuth(roles);
            }
        }
        return true;
    }

    public boolean manageable(List<Role> roles) {
        if (superMenu != null && !superMenu.manageable(roles)) {
            return false;
        }

        for (MenuAuthorization menuAuthorization : menuAuthorizations) {
            if(menuAuthorization.support(AuthorizationProperty.MANAGEABLE)) {
                return menuAuthorization.isAuth(roles);
            }
        }
        return true;
    }

    public boolean accessible(List<Role> roles) {
        if (superMenu != null && !superMenu.accessible(roles)) {
            return false;
        }

        for (MenuAuthorization menuAuthorization : menuAuthorizations) {
            if(menuAuthorization.support(AuthorizationProperty.ACCESS)) {
                return menuAuthorization.isAuth(roles);
            }
        }
        return true;
    }

    public boolean exposable(List<Role> roles) {

        if (superMenu != null && !superMenu.exposable(roles)) {
            return false;
        }

        for (MenuAuthorization menuAuthorization : menuAuthorizations) {
            if(menuAuthorization.support(AuthorizationProperty.EXPOSE)) {
                return menuAuthorization.isAuth(roles);
            }
        }
        return true;
    }
}
