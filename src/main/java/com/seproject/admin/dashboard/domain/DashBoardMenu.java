package com.seproject.admin.dashboard.domain;

import com.seproject.account.role.domain.Role;
import com.seproject.admin.menu.domain.SelectOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@Entity
@Builder
@AllArgsConstructor
public class DashBoardMenu {
    public static final String MENU_EDIT_URL = "/admin/menu";
    public static final String MENU_ADMIN_DASHBOARD_MENU_URL = "/admin/adminMenu";

    public static final String ACCOUNT_MANAGE_URL = "/admin/account";
    public static final String ACCOUNT_POLICY_URL = "/admin/accountPolicy";
    public static final String ROLE_MANAGE_URL = "/admin/roles";

    public static final String POST_MANAGE_URL = "/admin/posts";
    public static final String COMMENT_MANAGE_URL = "/admin/comments";
    public static final String FILE_MANAGE_URL = "/admin/files";
    public static final String TRASH_URL = "/admin/trash";

    public static final String GENERAL_URL = "/admin/general";
    public static final String MAIN_PAGE_MENU_MANAGE_URL = "/admin/mainPageMenu";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String url;

    @Enumerated(EnumType.STRING)
    private DashBoardMenuGroup menuGroup;

    @Enumerated(EnumType.STRING)
    private SelectOption selectOption;

    @OneToMany(mappedBy = "dashBoardMenu", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DashBoardMenuAuthorization> dashBoardMenuAuthorizations = new ArrayList<>();

    public boolean authorize(List<Role> roles) {

        for (DashBoardMenuAuthorization authorization : dashBoardMenuAuthorizations) {
            if(authorization.authorize(roles)) {
                return true;
            }
        }

        return false;
    }

    public void update(SelectOption selectOption, List<DashBoardMenuAuthorization> dashBoardMenuAuthorizations) {
        this.selectOption = selectOption;
        this.dashBoardMenuAuthorizations.clear();
        this.dashBoardMenuAuthorizations.addAll(dashBoardMenuAuthorizations);
    }


}
