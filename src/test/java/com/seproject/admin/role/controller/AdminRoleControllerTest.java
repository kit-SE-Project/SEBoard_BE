package com.seproject.admin.role.controller;

import com.seproject.global.IntegrationTestSupport;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class AdminRoleControllerTest extends IntegrationTestSupport {
//    static final String url = "/admin/roles/";
//
//    @Test
//    public void 권한_목록_조회() throws Exception {
//
//        for (int i = 0; i < 43; i++) {
//            roleSetup.createRole();
//        }
//
//        ResultActions perform = mvc.perform(get(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .header("Authorization",accessToken)
//                .characterEncoding("UTF-8")
//        );
//
//        perform.andDo(print())
//                .andExpect(status().isOk());
//
//        perform
//                .andExpect(jsonPath("$.size()").value(43 + 3));
//    }
//
//    @Test
//    public void 권한_생성() throws Exception {
//        RoleDTO.CreateRoleRequest request = new RoleDTO.CreateRoleRequest();
//        request.setName(UUID.randomUUID().toString().substring(0,8));
//        request.setDescription(UUID.randomUUID().toString());
//        request.setAlias(UUID.randomUUID().toString().substring(0,8));
//
//        mvc.perform(post(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .content(objectMapper.writeValueAsString(request))
//        )
//                .andDo(print())
//                .andExpect(status().isOk());
//
//        Role findRole = roleService.findByName("ROLE_" + request.getName());
//
//        assertEquals(findRole.getDescription(),request.getDescription());
//        assertEquals(findRole.getAuthority(),"ROLE_" + request.getName());
//        assertEquals(findRole.toString(),request.getAlias());
//    }
//
//    @Test
//    public void 기본_권한과_똑같은_권한_생성() throws Exception {
//        List<Role> immutableRoles = List.of(
//                roleSetup.getRoleAdmin(),
//                roleSetup.getRoleKumoh(),
//                roleSetup.getRoleUser());
//
//        for (Role immutableRole : immutableRoles) {
//            RoleDTO.CreateRoleRequest request = new RoleDTO.CreateRoleRequest();
//            request.setName(immutableRole.getAuthority());
//            request.setDescription(UUID.randomUUID().toString());
//            request.setAlias(UUID.randomUUID().toString().substring(0,8));
//
//            em.flush(); em.clear();
//            mvc.perform(post(url)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .accept(MediaType.APPLICATION_JSON)
//                            .characterEncoding("UTF-8")
//                            .header("Authorization",accessToken)
//                            .content(objectMapper.writeValueAsString(request))
//                    )
//                    .andDo(print())
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.code").value(ErrorCode.IMMUTABLE_ROLE.getCode()));
//        }
//    }
//
//    @Test
//    public void 권한_수정() throws Exception {
//        Role role = roleSetup.createRole();
//
//        RoleDTO.UpdateRoleRequest request = new RoleDTO.UpdateRoleRequest();
//        request.setName(UUID.randomUUID().toString().substring(0,8));
//        request.setDescription(UUID.randomUUID().toString());
//        request.setAlias(UUID.randomUUID().toString().substring(0,8));
//
//        em.flush(); em.clear();
//
//        mvc.perform(put(url + "{roleId}", role.getId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .header("Authorization",accessToken)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andDo(print())
//                .andExpect(status().isOk());
//
//        em.flush(); em.clear();
//
//        Role findRole = roleService.findById(role.getId());
//
//        assertEquals(findRole.getAuthority(),"ROLE_" + request.getName());
//        assertEquals(findRole.getDescription(),request.getDescription());
//        assertEquals(findRole.toString(),request.getAlias());
//    }
//
//    @Test
//    public void 수정할수_없는_권한_수정() throws Exception {
//        List<Role> immutableRoles = List.of(
//                roleSetup.getRoleAdmin(),
//                roleSetup.getRoleKumoh(),
//                roleSetup.getRoleUser());
//
//        for (Role immutableRole : immutableRoles) {
//            RoleDTO.UpdateRoleRequest request = new RoleDTO.UpdateRoleRequest();
//            request.setName(immutableRole.getAuthority());
//            request.setDescription(UUID.randomUUID().toString());
//            request.setAlias(UUID.randomUUID().toString().substring(0,8));
//
//            em.flush(); em.clear();
//            mvc.perform(put(url + "{roleId}", immutableRole.getId())
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .accept(MediaType.APPLICATION_JSON)
//                            .characterEncoding("UTF-8")
//                            .header("Authorization",accessToken)
//                            .content(objectMapper.writeValueAsString(request))
//                    )
//                    .andDo(print())
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.code").value(ErrorCode.IMMUTABLE_ROLE.getCode()));
//        }
//    }
//
//    @Test
//    public void 권한_삭제() throws Exception {
//        Role role = roleSetup.createRole();
//        em.flush(); em.clear();
//
//        mvc.perform(delete(url + "{roleId}", role.getId())
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .header("Authorization",accessToken)
//                .characterEncoding("UTF-8")
//        )
//                .andDo(print())
//                .andExpect(status().isOk());
//
//        em.flush(); em.clear();
//
//        CustomIllegalArgumentException ex = assertThrows(CustomIllegalArgumentException.class, () -> {
//            roleService.findById(role.getId());
//        });
//
//        assertEquals(ex.getErrorCode(),ErrorCode.ROLE_NOT_FOUND);
//    }
//
//    @Test
//    public void 삭제할수_없는_권한_삭제() throws Exception {
//        List<Role> immutableRoles = List.of(
//                roleSetup.getRoleAdmin(),
//                roleSetup.getRoleKumoh(),
//                roleSetup.getRoleUser());
//
//        for (Role immutableRole : immutableRoles) {
//            em.flush(); em.clear();
//            mvc.perform(delete(url + "{roleId}", immutableRole.getId())
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .accept(MediaType.APPLICATION_JSON)
//                            .header("Authorization",accessToken)
//                            .characterEncoding("UTF-8")
//                    )
//                    .andDo(print())
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.code").value(ErrorCode.IMMUTABLE_ROLE.getCode()));
//        }
//    }
//
//    @Autowired
//    AccountSetup accountSetup;
//
//    @Test
//    public void 권한_수정_후_메뉴_접근_가능() throws Exception {
//        Role role = roleSetup.createRole();
//        Role roleUser = roleSetup.getRoleUser();
//        Menu menu = menuSetup.createMenu();
//
//        assertTrue(menu.accessible(List.of(role))); assertTrue(menu.accessible(List.of(roleUser)));
//        assertTrue(menu.exposable(List.of(role))); assertTrue(menu.exposable(List.of(roleUser)));
//        assertTrue(menu.editable(List.of(role))); assertTrue(menu.editable(List.of(roleUser)));
//        assertTrue(menu.manageable(List.of(role))); assertTrue(menu.manageable(List.of(roleUser)));
//
//        MenuDTO.MenuAuthOption opt = new MenuDTO.MenuAuthOption();
//        opt.setOption(SelectOption.SELECT.getName());
//        opt.setRoles(List.of(role.getId()));
//
//        MenuDTO.MenuAuthOption[] input = new MenuDTO.MenuAuthOption[]{opt,opt,null,null};
//        MenuDTO.UpdateMenuRequest request = MenuRequestBuilder.getUpdateMenuRequest(input);
//
//        Account adminAccount = accountSetup.getAdminAccount();
//        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(adminAccount,adminAccount.getPassword(),adminAccount.getAuthorities());
//        SecurityContextHolder.getContext().setAuthentication(token);
//
//        adminMenuAppService.update(menu.getMenuId(),request);
//
//        em.flush(); em.clear();
//
//        Menu updatedMenu = menuService.findById(menu.getMenuId());
//        Role findRole = roleService.findById(role.getId());
//        Role findRoleUser = roleService.findById(roleUser.getId());
//
//        assertTrue(updatedMenu.accessible(List.of(findRole)));
//        assertTrue(updatedMenu.exposable(List.of(findRole)));
//
//        assertFalse(updatedMenu.accessible(List.of(findRoleUser)));
//        assertFalse(updatedMenu.exposable(List.of(findRoleUser)));
//
//        RoleDTO.UpdateRoleRequest updateRoleRequest = new RoleDTO.UpdateRoleRequest();
//        updateRoleRequest.setName(UUID.randomUUID().toString().substring(0,8));
//        updateRoleRequest.setDescription(UUID.randomUUID().toString());
//        updateRoleRequest.setAlias(UUID.randomUUID().toString().substring(0,8));
//
//        mvc.perform(put(url + "{roleId}", role.getId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .header("Authorization",accessToken)
//                        .content(objectMapper.writeValueAsString(updateRoleRequest)))
//                .andDo(print())
//                .andExpect(status().isOk());
//
//        em.flush(); em.clear();
//
//        updatedMenu = menuService.findById(menu.getMenuId());
//
//        Role updatedRole = roleService.findById(role.getId());
//        findRoleUser = roleService.findById(roleUser.getId());
//
//        assertTrue(updatedMenu.accessible(List.of(updatedRole)));
//        assertTrue(updatedMenu.exposable(List.of(updatedRole)));
//
//        assertFalse(updatedMenu.accessible(List.of(findRoleUser)));
//        assertFalse(updatedMenu.exposable(List.of(findRoleUser)));
//    }
//
//
}