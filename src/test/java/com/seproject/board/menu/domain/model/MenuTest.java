package com.seproject.board.menu.domain.model;

import com.seproject.account.authorization.domain.MenuExposeAuthorization;
import com.seproject.account.role.domain.Role;
import com.seproject.admin.menu.domain.SelectOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MenuTest {
    private Menu createMenu(SelectOption option, List<Role> roles){
        Menu menu = new Menu(null, null, "Menu", "Menu입니다.");
        MenuExposeAuthorization menuExposeAuthorization = new MenuExposeAuthorization();
        menuExposeAuthorization.setSelectOption(option);
        menuExposeAuthorization.update(roles);

        menu.addAuthorization(menuExposeAuthorization);

        return menu;
    }

    private Role createRole(Long id, String name){
        return Role.builder()
                .roleId(id)
                .name(name)
                .description("description")
                .alias(name)
                .build();
    }

    @DisplayName("Menu의 노출 권한 목록에 특정한 권한 옵션이 지정된 경우, Role 리스트에 해당 권한이 포함되어있어야 노출가능하다.")
    //필수적인 권한에 대해서는 일반적인 Role Entity 말고 다른 방식??
    @Test
    void exposable_certain_role(){
        //given
        Role adminRole = createRole(1L, Role.ROLE_ADMIN);
        Role userRole = createRole(2L, Role.ROLE_USER);
        Role kumohRole = createRole(3L, Role.ROLE_KUMOH);
        Role testRole = createRole(4L,"TEST");

        Menu menuAdmin = createMenu(SelectOption.ONLY_ADMIN, List.of(adminRole));
        Menu menuOverKumoh = createMenu(SelectOption.OVER_KUMOH, List.of(adminRole, kumohRole));
        Menu menuOverUser = createMenu(SelectOption.OVER_USER, List.of(adminRole, kumohRole, userRole));
        Menu menuOverSelect = createMenu(SelectOption.SELECT, List.of(testRole));
        Menu menuAll = createMenu(SelectOption.ALL, List.of());

        //when
        boolean exposable_admin_from_admin = menuAdmin.exposable(List.of(adminRole));
        boolean exposable_admin_from_user = menuAdmin.exposable(List.of(userRole));

        boolean exposable_kumoh_from_admin = menuOverKumoh.exposable(List.of(adminRole));
        boolean exposable_kumoh_from_kumoh = menuOverKumoh.exposable(List.of(kumohRole));
        boolean exposable_kumoh_from_user = menuOverKumoh.exposable(List.of(userRole));

        boolean exposable_user_from_admin = menuOverUser.exposable(List.of(adminRole));
        boolean exposable_user_from_kumoh = menuOverUser.exposable(List.of(kumohRole));
        boolean exposable_user_from_user = menuOverUser.exposable(List.of(userRole));
        boolean exposable_user_from_test = menuOverUser.exposable(List.of(testRole));

        boolean exposable_test_from_admin = menuOverSelect.exposable(List.of(adminRole));
        boolean exposable_test_from_kumoh = menuOverSelect.exposable(List.of(kumohRole));
        boolean exposable_test_from_user = menuOverSelect.exposable(List.of(userRole));
        boolean exposable_test_from_test = menuOverSelect.exposable(List.of(testRole));

        //then
        assertThat(exposable_admin_from_admin).isTrue();
        assertThat(exposable_admin_from_user).isFalse();

        assertThat(exposable_kumoh_from_admin).isTrue();
        assertThat(exposable_kumoh_from_kumoh).isTrue();
        assertThat(exposable_kumoh_from_user).isFalse();

        assertThat(exposable_user_from_admin).isTrue();
        assertThat(exposable_user_from_kumoh).isTrue();
        assertThat(exposable_user_from_user).isTrue();
        assertThat(exposable_user_from_test).isFalse();

        assertThat(exposable_test_from_admin).isFalse();
        assertThat(exposable_test_from_kumoh).isFalse();
        assertThat(exposable_test_from_user).isFalse();
        assertThat(exposable_test_from_test).isTrue();

        assertThat(menuAll.exposable(List.of(adminRole))).isTrue();
        assertThat(menuAll.exposable(List.of(userRole))).isTrue();
        assertThat(menuAll.exposable(List.of(kumohRole))).isTrue();
        assertThat(menuAll.exposable(List.of(testRole))).isTrue();
        assertThat(menuAll.exposable(List.of())).isTrue();
        assertThat(menuAll.exposable(null)).isTrue();
    }

    @DisplayName("Menu의 접근 권한 목록에 특정한 권한 옵션이 지정된 경우, Role 리스트에 해당 권한이 포함되어있어야 노출가능하다.")
    //필수적인 권한에 대해서는 일반적인 Role Entity 말고 다른 방식??
    @Test
    void accessible_certain_role(){
        //given
        Role adminRole = createRole(1L, Role.ROLE_ADMIN);
        Role userRole = createRole(2L, Role.ROLE_USER);
        Role kumohRole = createRole(3L, Role.ROLE_KUMOH);
        Role testRole = createRole(4L,"TEST");

        Menu menuAdmin = createMenu(SelectOption.ONLY_ADMIN, List.of(adminRole));
        Menu menuOverKumoh = createMenu(SelectOption.OVER_KUMOH, List.of(adminRole, kumohRole));
        Menu menuOverUser = createMenu(SelectOption.OVER_USER, List.of(adminRole, kumohRole, userRole));
        Menu menuOverSelect = createMenu(SelectOption.SELECT, List.of(testRole));
        Menu menuAll = createMenu(SelectOption.ALL, List.of());

        //when
        boolean exposable_admin_from_admin = menuAdmin.exposable(List.of(adminRole));
        boolean exposable_admin_from_user = menuAdmin.exposable(List.of(userRole));

        boolean exposable_kumoh_from_admin = menuOverKumoh.exposable(List.of(adminRole));
        boolean exposable_kumoh_from_kumoh = menuOverKumoh.exposable(List.of(kumohRole));
        boolean exposable_kumoh_from_user = menuOverKumoh.exposable(List.of(userRole));

        boolean exposable_user_from_admin = menuOverUser.exposable(List.of(adminRole));
        boolean exposable_user_from_kumoh = menuOverUser.exposable(List.of(kumohRole));
        boolean exposable_user_from_user = menuOverUser.exposable(List.of(userRole));
        boolean exposable_user_from_test = menuOverUser.exposable(List.of(testRole));

        boolean exposable_test_from_admin = menuOverSelect.exposable(List.of(adminRole));
        boolean exposable_test_from_kumoh = menuOverSelect.exposable(List.of(kumohRole));
        boolean exposable_test_from_user = menuOverSelect.exposable(List.of(userRole));
        boolean exposable_test_from_test = menuOverSelect.exposable(List.of(testRole));

        //then
        assertThat(exposable_admin_from_admin).isTrue();
        assertThat(exposable_admin_from_user).isFalse();

        assertThat(exposable_kumoh_from_admin).isTrue();
        assertThat(exposable_kumoh_from_kumoh).isTrue();
        assertThat(exposable_kumoh_from_user).isFalse();

        assertThat(exposable_user_from_admin).isTrue();
        assertThat(exposable_user_from_kumoh).isTrue();
        assertThat(exposable_user_from_user).isTrue();
        assertThat(exposable_user_from_test).isFalse();

        assertThat(exposable_test_from_admin).isFalse();
        assertThat(exposable_test_from_kumoh).isFalse();
        assertThat(exposable_test_from_user).isFalse();
        assertThat(exposable_test_from_test).isTrue();

        assertThat(menuAll.exposable(List.of(adminRole))).isTrue();
        assertThat(menuAll.exposable(List.of(userRole))).isTrue();
        assertThat(menuAll.exposable(List.of(kumohRole))).isTrue();
        assertThat(menuAll.exposable(List.of(testRole))).isTrue();
        assertThat(menuAll.exposable(List.of())).isTrue();
        assertThat(menuAll.exposable(null)).isTrue();
    }
}