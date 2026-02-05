package com.seproject.account.role.service;

import com.seproject.account.role.domain.Role;
import com.seproject.admin.menu.domain.SelectOption;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;
import com.seproject.global.IntegrationTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoleServiceTest extends IntegrationTestSupport {
//
//    @Test
//    public void 모든_권한_조회() throws Exception {
//        Role newRole = roleSetup.createRole();
//        List<Role> all = roleService.findAll();
//        Role roleAdmin = roleSetup.getRoleAdmin();
//        Role roleKumoh = roleSetup.getRoleKumoh();
//        Role roleUser = roleSetup.getRoleUser();
//        Assertions.assertEquals(all.size(),4);
//        Assertions.assertTrue(all.contains(roleAdmin));
//        Assertions.assertTrue(all.contains(roleKumoh));
//        Assertions.assertTrue(all.contains(roleUser));
//        Assertions.assertTrue(all.contains(newRole));
//    }
//
//    @Test
//    public void ID리스트로_조회() throws Exception {
//        Role roleAdmin = roleSetup.getRoleAdmin();
//        Role roleKumoh = roleSetup.getRoleKumoh();
//
//        List<Long> id = List.of(roleAdmin.getId(), roleKumoh.getId());
//
//        List<Role> roles = roleService.findByIds(id);
//
//        Assertions.assertEquals(roles.size(),2);
//        Assertions.assertTrue(roles.contains(roleAdmin));
//        Assertions.assertTrue(roles.contains(roleKumoh));
//    }
//
//    @Test
//    public void ID리스트로_조회_없는_권한() throws Exception {
//        Role roleAdmin = roleSetup.getRoleAdmin();
//        Role roleKumoh = roleSetup.getRoleKumoh();
//
//        List<Long> id = List.of(roleAdmin.getId(), roleKumoh.getId(),3132L);
//
//        Assertions.assertThrows(CustomIllegalArgumentException.class, () -> {
//            List<Role> roles = roleService.findByIds(id);
//        });
//
//    }
//
//    @Test
//    public void id로_조회() throws Exception {
//        Role role = roleSetup.createRole();
//        Role findRole = roleService.findById(role.getId());
//        Assertions.assertEquals(role,findRole);
//    }
//
//    @Test
//    public void 없는_id로_조회() throws Exception {
//        Assertions.assertThrows(CustomIllegalArgumentException.class, () -> {
//            Role findRole = roleService.findById(42394L);
//        });
//    }
//
//    @Test
//    public void 이름으로_조회() throws Exception {
//        Role role = roleSetup.createRole();
//        Role findRole = roleService.findByName(role.getAuthority());
//        Assertions.assertEquals(role,findRole);
//    }
//
//    @Test
//    public void 없는_이름으로_조회() throws Exception {
//        Assertions.assertThrows(CustomIllegalArgumentException.class, () -> {
//            Role byName = roleService.findByName(UUID.randomUUID().toString());
//        });
//    }
//
//    @Test
//    public void 이름_목록으로_조회() throws Exception {
//        List<Role> roles = new ArrayList<>();
//        List<String> roleNames = new ArrayList<>();
//        for (int i = 0; i < 100; i++) {
//            Role role = roleSetup.createRole();
//            roles.add(role);
//            roleNames.add(role.getAuthority());
//        }
//
//        List<Role> result = roleService.findByNameIn(roleNames);
//        Assertions.assertEquals(result.size(), 100);
//
//        for (int i = 0; i < 100; i++) {
//            Assertions.assertTrue(result.contains(roles.get(i)));
//        }
//    }
//
//    @Test
//    public void 권한_생성_테스트() throws Exception {
//        String roleName = UUID.randomUUID().toString();
//        String description = UUID.randomUUID().toString();
//        String alias = UUID.randomUUID().toString();
//        Long roleId = roleService.createRole(roleName, description, alias);
//
//        Role findRole = roleService.findById(roleId);
//        Assertions.assertTrue(findRole.getAuthority().startsWith("ROLE_"));
//
//        Assertions.assertEquals(findRole.getAuthority(),"ROLE_" + roleName);
//        Assertions.assertEquals(findRole.getDescription(), description);
//        Assertions.assertEquals(findRole.toString(),alias);
//    }
//
//    @Test
//    public void 권한_삭제_테스트() throws Exception {
//        Role role = roleSetup.createRole();
//
//        Long id = role.getId();
//        roleService.deleteRole(id);
//        Assertions.assertThrows(CustomIllegalArgumentException.class, () -> {
//            roleService.findById(id);
//        });
//    }
//
//    @Test
//    public void 권한_삭제_불가능한_권한() throws Exception {
//        Role roleUser = roleSetup.getRoleUser();
//        Role roleKumoh = roleSetup.getRoleKumoh();
//        Role roleAdmin = roleSetup.getRoleAdmin();
//        List<Role> immutables = List.of(roleUser, roleKumoh, roleAdmin);
//
//        for (Role immutable : immutables) {
//            Assertions.assertTrue(immutable.isImmutable());
//            Assertions.assertThrows(CustomIllegalArgumentException.class,()->{
//                roleService.deleteRole(immutable.getId());
//            });
//        }
//    }
//
//    @Test
//    public void 권한_삭제_존재하지_않음() throws Exception {
//        Assertions.assertThrows(CustomIllegalArgumentException.class,() -> {
//            roleService.deleteRole(42342309L);
//        });
//    }
//
//    @Test
//    public void 권한_수정_테스트() throws Exception {
//        Role role = roleSetup.createRole();
//        em.flush();
//        em.clear();
//
//        Long id = role.getId();
//        String roleName = UUID.randomUUID().toString();
//        String description = UUID.randomUUID().toString();
//        String alias = UUID.randomUUID().toString();
//
//        roleService.updateRole(id, roleName, description, alias);
//        Role findRole = roleService.findById(id);
//
//        Assertions.assertNotEquals(findRole.getAuthority(),role.getAuthority());
//        Assertions.assertNotEquals(findRole.getDescription(),role.getDescription());
//        Assertions.assertNotEquals(findRole.toString(),role.toString());
//
//        Assertions.assertEquals(findRole.getAuthority(),"ROLE_" + roleName);
//        Assertions.assertEquals(findRole.getDescription(),description);
//        Assertions.assertEquals(findRole.toString(),alias);
//    }
//
//    @Test
//    public void 권한_수정_불가능한_권한() throws Exception {
//        Role roleUser = roleSetup.getRoleUser();
//        Role roleKumoh = roleSetup.getRoleKumoh();
//        Role roleAdmin = roleSetup.getRoleAdmin();
//        List<Role> immutables = List.of(roleUser, roleKumoh, roleAdmin);
//
//        for (Role immutable : immutables) {
//            Assertions.assertTrue(immutable.isImmutable());
//            Assertions.assertThrows(CustomIllegalArgumentException.class,()->{
//                String name = UUID.randomUUID().toString();
//                String description = UUID.randomUUID().toString();
//                String alias = UUID.randomUUID().toString();
//                roleService.updateRole(immutable.getId(),name,description,alias);
//            });
//        }
//    }
//
//    @Test
//    public void 권한_수정_존재하지_않음() throws Exception {
//        Assertions.assertThrows(CustomIllegalArgumentException.class,() -> {
//            roleService.updateRole(42342309L,"123123","#$%$#","%#@$%324");
//        });
//    }
//
//    @Test
//    public void 권한_매핑() throws Exception {
//        Role roleAdmin = roleSetup.getRoleAdmin();
//        Role roleKumoh = roleSetup.getRoleKumoh();
//        Role roleUser = roleSetup.getRoleUser();
//
//        List<Role> allRoles = roleService.convertRoles(SelectOption.ALL);
//        Assertions.assertTrue(allRoles.isEmpty());
//
//        List<Role> selectRoles = roleService.convertRoles(SelectOption.SELECT);
//        Assertions.assertTrue(selectRoles.isEmpty());
//
//        List<Role> overUserRoles = roleService.convertRoles(SelectOption.OVER_USER);
//        Assertions.assertEquals(overUserRoles.size() , 3);
//        Assertions.assertTrue(overUserRoles.contains(roleUser));
//        Assertions.assertTrue(overUserRoles.contains(roleKumoh));
//        Assertions.assertTrue(overUserRoles.contains(roleAdmin));
//
//        List<Role> overKumohRoles = roleService.convertRoles(SelectOption.OVER_KUMOH);
//        Assertions.assertEquals(overKumohRoles.size() , 2);
//        Assertions.assertTrue(overKumohRoles.contains(roleKumoh));
//        Assertions.assertTrue(overKumohRoles.contains(roleAdmin));
//
//        List<Role> onlyAdminRoles = roleService.convertRoles(SelectOption.ONLY_ADMIN);
//        Assertions.assertEquals(onlyAdminRoles.size() , 1);
//        Assertions.assertTrue(onlyAdminRoles.contains(roleAdmin));
//    }
//
//    @Test
//    public void 권한_매핑_실패() throws Exception {
//        CustomIllegalArgumentException e = assertThrows(CustomIllegalArgumentException.class, () -> {
//            List<Role> allRoles = roleService.convertRoles(null);
//        });
//
//        assertEquals(e.getErrorCode(), ErrorCode.SELECT_OPTION_NOT_FOUND);
//    }
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
}