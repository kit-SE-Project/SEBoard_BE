package com.seproject.board.post.controller;

import com.seproject.global.IntegrationTestSupport;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class PostControllerTest extends IntegrationTestSupport {
//
//    static final String url = "/posts/";
//
//    @Test
//    public void 게시글_목록_조회() throws Exception {
//
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        for (int i = 0; i < 43; i++) {
//            postSetup.createPost(member, category);
//        }
//
//        em.flush(); em.clear();
//
//        mvc.perform(get(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .queryParam("categoryId", String.valueOf(category.getMenuId()))
//                .queryParam("page" , "0")
//                .queryParam("perPage", "50")
//        ).andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content.size()").value(43))
//                .andExpect(jsonPath("$.empty").value(false))
//                .andExpect(jsonPath("$.first").value(true))
//                .andExpect(jsonPath("$.last").value(true))
//                .andExpect(jsonPath("$.number").value(0))
//                .andExpect(jsonPath("$.totalElements").value(43))
//                .andExpect(jsonPath("$.numberOfElements").value(43))
//                .andExpect(jsonPath("$.size").value(50));
//    }
//    @Test
//    public void 게시글_카테고리_비로그인_권한_없음() throws Exception {
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//
//        MenuDTO.MenuAuthOption edit = new MenuDTO.MenuAuthOption();
//        edit.setOption(SelectOption.SELECT.getName());
//        edit.setRoles(List.of(roleSetup.createRole().getId()));
//
//        MenuDTO.MenuAuthOption[] input = new MenuDTO.MenuAuthOption[] {null,null,edit,edit};
//        MenuDTO.UpdateMenuRequest req = MenuRequestBuilder.getUpdateMenuRequest(input);
//
//        Account adminAccount = accountSetup.getAdminAccount();
//
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(adminAccount,
//                        null,adminAccount.getAuthorities()));
//
//        adminMenuAppService.update(category.getMenuId(),req);
//
//        em.flush(); em.clear();
//
//        mvc.perform(get(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .queryParam("categoryId", String.valueOf(category.getMenuId()))
//                .queryParam("page" , "0")
//                .queryParam("perPage", "50")
//        ).andDo(print())
//                .andExpect(status().isForbidden());
//    }
//    @Test
//    public void 게시글_카테고리_로그인_권한_없음() throws Exception {
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//
//        MenuDTO.MenuAuthOption edit = new MenuDTO.MenuAuthOption();
//        edit.setOption(SelectOption.SELECT.getName());
//        edit.setRoles(List.of(roleSetup.createRole().getId()));
//
//        MenuDTO.MenuAuthOption[] input = new MenuDTO.MenuAuthOption[] {null,null,edit,edit};
//        MenuDTO.UpdateMenuRequest req = MenuRequestBuilder.getUpdateMenuRequest(input);
//
//        Account adminAccount = accountSetup.getAdminAccount();
//
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(adminAccount,
//                        null,adminAccount.getAuthorities()));
//
//        adminMenuAppService.update(category.getMenuId(),req);
//
//        em.flush(); em.clear();
//
//        mvc.perform(get(url)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .queryParam("categoryId", String.valueOf(category.getMenuId()))
//                        .queryParam("page" , "0")
//                        .queryParam("perPage", "50")
//                        .header("Authorization",tokenSetup.getAccessToken(adminAccount))
//                ).andDo(print())
//                .andExpect(status().isForbidden());
//    }
//    @Test
//    public void 게시글_상세_조회() throws Exception {
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        Post post = postSetup.createPost(member, category);
//
//        em.flush(); em.clear();
//
//        mvc.perform(get(url + post.getPostId())
//                .accept(MediaType.APPLICATION_JSON)
//                .contentType(MediaType.APPLICATION_JSON)
//                .header("Authorization", tokenSetup.getAccessToken(formAccount))
//                .characterEncoding("UTF-8")
//        ).andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.postId").value(post.getPostId()))
//                .andExpect(jsonPath("$.title").value(post.getTitle()))
//                .andExpect(jsonPath("$.contents").value(post.getContents()))
//                .andExpect(jsonPath("$.category.categoryId").value(category.getMenuId()))
//                .andExpect(jsonPath("$.category.name").value(category.getName()))
//                .andExpect(jsonPath("$.author.loginId").value(member.getLoginId()))
//                .andExpect(jsonPath("$.author.name").value(member.getName()))
//                .andExpect(jsonPath("$.views").value(post.getViews()));
//
//    }
//    @Test
//    public void 게시글_상세_조회_비로그인_권한_없음() throws Exception {
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        Post post = postSetup.createPost(member, category);
//        post.changeExposeOption(ExposeOption.of(ExposeState.KUMOH,null));
//
//        MenuDTO.MenuAuthOption edit = new MenuDTO.MenuAuthOption();
//        edit.setOption(SelectOption.SELECT.getName());
//        edit.setRoles(List.of(roleSetup.getRoleAdmin().getId()));
//
//        MenuDTO.MenuAuthOption[] input = new MenuDTO.MenuAuthOption[] {null,null,edit,edit};
//        MenuDTO.UpdateMenuRequest req = MenuRequestBuilder.getUpdateMenuRequest(input);
//
//        Account adminAccount = accountSetup.getAdminAccount();
//        Member adminMember = boardUserSetup.createMember(adminAccount);
//
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(adminAccount,
//                        null,adminAccount.getAuthorities()));
//
//        adminMenuAppService.update(category.getMenuId(),req);
//
//        SecurityContextHolder.getContext().setAuthentication(null);
//
//        em.flush(); em.clear();
//
//        mvc.perform(get(url + post.getPostId())
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                ).andDo(print())
//                .andExpect(status().isUnauthorized());
//    }
//    @Test
//    public void 게시글_상세_조회_로그인_권한_없음() throws Exception {
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        FormAccount reader = accountSetup.createFormAccount();
//        Member readerMember = boardUserSetup.createMember(reader);
//
//        Post post = postSetup.createPost(member, category);
//        post.changeExposeOption(ExposeOption.of(ExposeState.KUMOH,null));
//
//        MenuDTO.MenuAuthOption edit = new MenuDTO.MenuAuthOption();
//        edit.setOption(SelectOption.SELECT.getName());
//        edit.setRoles(List.of(roleSetup.createRole().getId()));
//
//        MenuDTO.MenuAuthOption[] input = new MenuDTO.MenuAuthOption[] {null,null,edit,edit};
//        MenuDTO.UpdateMenuRequest req = MenuRequestBuilder.getUpdateMenuRequest(input);
//
//        Account adminAccount = accountSetup.getAdminAccount();
//
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(adminAccount,
//                        null,adminAccount.getAuthorities()));
//
//        adminMenuAppService.update(category.getMenuId(),req);
//
//        SecurityContextHolder.getContext().setAuthentication(null);
//
//        em.flush(); em.clear();
//
//        mvc.perform(get(url + post.getPostId())
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .header("Authorization",tokenSetup.getAccessToken(reader))
//                ).andDo(print())
//                .andExpect(status().isForbidden());
//    }
//    @Test
//    public void 게시글_상세_조회_권한_없어도_내가_작성한_글은_조회_가능() throws Exception {
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        Post post = postSetup.createPost(member, category);
//        post.changeExposeOption(ExposeOption.of(ExposeState.KUMOH,null));
//
//        MenuDTO.MenuAuthOption edit = new MenuDTO.MenuAuthOption();
//        edit.setOption(SelectOption.SELECT.getName());
//        edit.setRoles(List.of(roleSetup.createRole().getId()));
//
//        MenuDTO.MenuAuthOption[] input = new MenuDTO.MenuAuthOption[] {null,null,edit,edit};
//        MenuDTO.UpdateMenuRequest req = MenuRequestBuilder.getUpdateMenuRequest(input);
//
//        Account adminAccount = accountSetup.getAdminAccount();
//
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(adminAccount,
//                        null,adminAccount.getAuthorities()));
//
//        adminMenuAppService.update(category.getMenuId(),req);
//
//        SecurityContextHolder.getContext().setAuthentication(null);
//
//        em.flush(); em.clear();
//
//        mvc.perform(get(url + post.getPostId())
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .header("Authorization",tokenSetup.getAccessToken(formAccount))
//                ).andDo(print())
//                .andExpect(status().isOk());
//    }
//    @Test
//    public void 비밀글_상세_조회() throws Exception {
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        FormAccount reader = accountSetup.createFormAccount();
//        Member readerMember = boardUserSetup.createMember(reader);
//
//        Post post = postSetup.createPost(member, category);
//        String password = UUID.randomUUID().toString();
//        post.changeExposeOption(ExposeOption.of(ExposeState.PRIVACY,password));
//
//        PostRequest.RetrievePrivacyPostRequest request = new PostRequest.RetrievePrivacyPostRequest();
//        request.setPassword(password);
//
//        em.flush(); em.clear();
//
//        mvc.perform(post(url + post.getPostId() + "/auth")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization", tokenSetup.getAccessToken(reader))
//                .content(objectMapper.writeValueAsString(request))
//        ).andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.postId").value(post.getPostId()))
//                .andExpect(jsonPath("$.title").value(post.getTitle()))
//                .andExpect(jsonPath("$.contents").value(post.getContents()))
//                .andExpect(jsonPath("$.category.categoryId").value(category.getMenuId()))
//                .andExpect(jsonPath("$.category.name").value(category.getName()))
//                .andExpect(jsonPath("$.author.loginId").value(member.getLoginId()))
//                .andExpect(jsonPath("$.author.name").value(member.getName()));
//    }
//    @Test
//    public void 비밀글_상세_조회_비밀번호_틀림() throws Exception {
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        FormAccount reader = accountSetup.createFormAccount();
//        Member readerMember = boardUserSetup.createMember(reader);
//
//        Post post = postSetup.createPost(member, category);
//        String password = UUID.randomUUID().toString();
//        post.changeExposeOption(ExposeOption.of(ExposeState.PRIVACY,password));
//
//        PostRequest.RetrievePrivacyPostRequest request = new PostRequest.RetrievePrivacyPostRequest();
//        request.setPassword("1234");
//
//        MenuDTO.MenuAuthOption edit = new MenuDTO.MenuAuthOption();
//        edit.setOption(SelectOption.SELECT.getName());
//        edit.setRoles(List.of(roleSetup.getRoleAdmin().getId()));
//
//        MenuDTO.MenuAuthOption[] input = new MenuDTO.MenuAuthOption[] {null,null,edit,edit};
//        MenuDTO.UpdateMenuRequest req = MenuRequestBuilder.getUpdateMenuRequest(input);
//
//        Account adminAccount = accountSetup.getAdminAccount();
//        Member adminMember = boardUserSetup.createMember(adminAccount);
//
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(adminAccount,
//                        null,adminAccount.getAuthorities()));
//
//        adminMenuAppService.update(category.getMenuId(),req);
//
//        SecurityContextHolder.getContext().setAuthentication(null);
//
//        em.flush(); em.clear();
//
//        mvc.perform(post(url + post.getPostId() + "/auth")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .header("Authorization", tokenSetup.getAccessToken(reader))
//                        .content(objectMapper.writeValueAsString(request))
//                ).andDo(print())
//                .andExpect(status().isBadRequest());
//    }
//    @Test
//    public void 비밀글_상세_조회_관리자() throws Exception {
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        FormAccount reader = accountSetup.createFormAccount();
//        Member readerMember = boardUserSetup.createMember(reader);
//
//        Post post = postSetup.createPost(member, category);
//        String password = UUID.randomUUID().toString();
//        post.changeExposeOption(ExposeOption.of(ExposeState.PRIVACY,password));
//
//        PostRequest.RetrievePrivacyPostRequest request = new PostRequest.RetrievePrivacyPostRequest();
//        request.setPassword("1234");
//
//        MenuDTO.MenuAuthOption edit = new MenuDTO.MenuAuthOption();
//        edit.setOption(SelectOption.SELECT.getName());
//        edit.setRoles(List.of(roleSetup.getRoleAdmin().getId()));
//
//        MenuDTO.MenuAuthOption[] input = new MenuDTO.MenuAuthOption[] {null,null,edit,edit};
//        MenuDTO.UpdateMenuRequest req = MenuRequestBuilder.getUpdateMenuRequest(input);
//
//        Account adminAccount = accountSetup.getAdminAccount();
//        Member adminMember = boardUserSetup.createMember(adminAccount);
//
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(adminAccount,
//                        null,adminAccount.getAuthorities()));
//
//        adminMenuAppService.update(category.getMenuId(),req);
//
//        SecurityContextHolder.getContext().setAuthentication(null);
//
//
//        em.flush(); em.clear();
//
//        mvc.perform(post(url + post.getPostId() + "/auth")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .header("Authorization", tokenSetup.getAccessToken(adminAccount))
//                        .content(objectMapper.writeValueAsString(request))
//                ).andDo(print())
//                .andExpect(status().isOk());
//    }
//    @Test
//    public void 비밀글_상세_조회_작성자() throws Exception {
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        FormAccount reader = accountSetup.createFormAccount();
//        Member readerMember = boardUserSetup.createMember(reader);
//
//        Post post = postSetup.createPost(member, category);
//        String password = UUID.randomUUID().toString();
//        post.changeExposeOption(ExposeOption.of(ExposeState.PRIVACY,password));
//
//        PostRequest.RetrievePrivacyPostRequest request = new PostRequest.RetrievePrivacyPostRequest();
//        request.setPassword("1234");
//
//        em.flush(); em.clear();
//
//        mvc.perform(post(url + post.getPostId() + "/auth")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .header("Authorization", tokenSetup.getAccessToken(reader))
//                        .content(objectMapper.writeValueAsString(request))
//                ).andDo(print())
//                .andExpect(status().isOk());
//    }
//    @Test
//    public void 공지_게시글_조회() throws Exception {
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        for (int i = 0; i < 4; i++) {
//            Post post = postSetup.createPost(member, category);
//            post.changePin(true);
//        }
//
//        em.flush(); em.clear();
//
//        mvc.perform(get(url + "pined")
//                .accept(MediaType.APPLICATION_JSON)
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .queryParam("categoryId",String.valueOf(category.getMenuId()))
//        ).andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.size()").value(4));
//    }
//    @Test
//    public void 공지_게시글_조회_권한_없음() throws Exception {
//        Menu menu = menuSetup.createMenu();
//        BoardMenu boardMenu = menuSetup.createBoardMenu(menu);
//        Category category = menuSetup.createCategory(boardMenu);
//
//        MenuDTO.MenuAuthOption expose = new MenuDTO.MenuAuthOption();
//        expose.setOption(SelectOption.SELECT.getName());
//        expose.setRoles(List.of(roleSetup.getRoleAdmin().getId()));
//
//        MenuDTO.MenuAuthOption[] input = new MenuDTO.MenuAuthOption[] {expose,expose,expose,expose};
//        MenuDTO.UpdateMenuRequest req = MenuRequestBuilder.getUpdateMenuRequest(input);
//
//        Account adminAccount = accountSetup.getAdminAccount();
//
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(adminAccount,
//                        null,adminAccount.getAuthorities()));
//
//        adminMenuAppService.update(menu.getMenuId(),req);
//
//        SecurityContextHolder.getContext().setAuthentication(null);
//
//        em.flush(); em.clear();
//
//        mvc.perform(get(url + "pined")
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .queryParam("categoryId",String.valueOf(category.getMenuId()))
//                ).andDo(print())
//                .andExpect(status().isForbidden());
//    }
//    @Test
//    public void 게시글_작성() throws Exception {
//
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//
//        em.flush(); em.clear();
//
//        PostRequest.CreatePostRequest request = new PostRequest.CreatePostRequest();
//        request.setTitle(UUID.randomUUID().toString());
//        request.setContents(UUID.randomUUID().toString());
//        request.setCategoryId(category.getMenuId());
//        request.setPined(false);
//
//        request.setExposeOption(new ExposeOptionRequest("PUBLIC"));
//        request.setAnonymous(false);
//
//        String result = mvc.perform(post(url)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .content(objectMapper.writeValueAsString(request))
//                        .header("Authorization", tokenSetup.getAccessToken(formAccount))
//                ).andDo(print())
//                .andExpect(status().isCreated())
//                .andReturn().getResponse()
//                .getContentAsString();
//
//        em.flush(); em.clear();
//
//        Integer id = JsonPath.parse(result).read("$.id");
//        Post findPost = postService.findById((long)id);
//
//        Assertions.assertEquals(findPost.getTitle(),request.getTitle());
//        Assertions.assertEquals(findPost.getContents(),request.getContents());
//        Assertions.assertEquals(findPost.getCategory().getMenuId(),request.getCategoryId());
//        Assertions.assertEquals(findPost.isPined(),request.isPined());
//        Assertions.assertEquals(findPost.getExposeOption().getExposeState().name(),request.getExposeOption().getName());
//        Assertions.assertTrue(findPost.isNamed());
//
//        BoardUser author = findPost.getAuthor();
//        Assertions.assertFalse(author.isAnonymous());
//        Assertions.assertTrue(author.isOwnAccountId(formAccount.getAccountId()));
//
//    }
//    @Test
//    public void 게시글_작성_권한_없음() throws Exception {
//
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//
//        MenuDTO.MenuAuthOption edit = new MenuDTO.MenuAuthOption();
//        edit.setOption(SelectOption.SELECT.getName());
//        edit.setRoles(List.of(roleSetup.getRoleAdmin().getId()));
//
//        MenuDTO.MenuAuthOption[] input = new MenuDTO.MenuAuthOption[] {null,null,edit,edit};
//        MenuDTO.UpdateMenuRequest req = MenuRequestBuilder.getUpdateMenuRequest(input);
//
//        Account adminAccount = accountSetup.getAdminAccount();
//
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(adminAccount,
//                        null,adminAccount.getAuthorities()));
//
//        adminMenuAppService.update(category.getMenuId(),req);
//
//        SecurityContextHolder.getContext().setAuthentication(null);
//
//        em.flush(); em.clear();
//
//        PostRequest.CreatePostRequest request = new PostRequest.CreatePostRequest();
//        request.setTitle(UUID.randomUUID().toString());
//        request.setContents(UUID.randomUUID().toString());
//        request.setCategoryId(category.getMenuId());
//        request.setPined(false);
//
//        request.setExposeOption(new ExposeOptionRequest("PUBLIC"));
//        request.setAnonymous(false);
//
//        mvc.perform(post(url)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .content(objectMapper.writeValueAsString(request))
//                        .header("Authorization", tokenSetup.getAccessToken(formAccount))
//                ).andDo(print())
//                .andExpect(status().isForbidden());
//    }
//    @Test
//    public void 게시글_작성_금오_권한_없음() throws Exception {
//
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//
//        em.flush(); em.clear();
//
//        PostRequest.CreatePostRequest request = new PostRequest.CreatePostRequest();
//        request.setTitle(UUID.randomUUID().toString());
//        request.setContents(UUID.randomUUID().toString());
//        request.setCategoryId(category.getMenuId());
//        request.setPined(false);
//
//        request.setExposeOption(new ExposeOptionRequest("KUMOH"));
//        request.setAnonymous(false);
//
//        mvc.perform(post(url)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .content(objectMapper.writeValueAsString(request))
//                        .header("Authorization", tokenSetup.getAccessToken(formAccount))
//                ).andDo(print())
//                .andExpect(status().isForbidden());
//    }
//    @Test
//    public void 게시글_수정() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//
//        Post post = postSetup.createPost(member, category);
//
//        em.flush(); em.clear();
//
//        PostRequest.UpdatePostRequest request = new PostRequest.UpdatePostRequest();
//        request.setTitle(UUID.randomUUID().toString());
//        request.setContents(UUID.randomUUID().toString());
//        request.setCategoryId(category.getMenuId());
//        request.setPined(false);
//        request.setExposeOption(new ExposeOptionRequest("PRIVACY","1234"));
//
//        mvc.perform(put(url + post.getPostId())
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .header("Authorization",tokenSetup.getAccessToken(formAccount))
//                .content(objectMapper.writeValueAsString(request))
//                .characterEncoding("UTF-8")
//        ).andDo(print())
//                .andExpect(status().isOk());
//
//        em.flush(); em.clear();
//
//        Post findPost = postService.findById(post.getPostId());
//
//        Assertions.assertEquals(request.getTitle(),findPost.getTitle());
//        Assertions.assertEquals(request.getContents(),findPost.getContents());
//        Assertions.assertEquals(request.getCategoryId(),findPost.getCategory().getMenuId());
//        Assertions.assertFalse(request.isPined());
//        Assertions.assertEquals("PRIVACY",findPost.getExposeOption().getExposeState().name());
//        Assertions.assertEquals("1234",findPost.getExposeOption().getPassword());
//    }
//    @Test
//    public void 게시글_수정_내가_작성한거_아님() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//
//        Post post = postSetup.createPost(member, category);
//
//        Account adminAccount = accountSetup.getAdminAccount();
//        em.flush(); em.clear();
//
//        PostRequest.UpdatePostRequest request = new PostRequest.UpdatePostRequest();
//        request.setTitle(UUID.randomUUID().toString());
//        request.setContents(UUID.randomUUID().toString());
//        request.setCategoryId(category.getMenuId());
//        request.setPined(false);
//        request.setExposeOption(new ExposeOptionRequest("PRIVACY","1234"));
//
//        mvc.perform(put(url + post.getPostId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .header("Authorization",tokenSetup.getAccessToken(adminAccount))
//                        .content(objectMapper.writeValueAsString(request))
//                        .characterEncoding("UTF-8")
//                ).andDo(print())
//                .andExpect(status().isForbidden());
//    }
//    @Test
//    public void 게시글_수정_권한_없음() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//
//        Post post = postSetup.createPost(member, category);
//
//        MenuDTO.MenuAuthOption edit = new MenuDTO.MenuAuthOption();
//        edit.setOption(SelectOption.SELECT.getName());
//        edit.setRoles(List.of(roleSetup.getRoleAdmin().getId()));
//
//        MenuDTO.MenuAuthOption[] input = new MenuDTO.MenuAuthOption[] {null,null,edit,edit};
//        MenuDTO.UpdateMenuRequest req = MenuRequestBuilder.getUpdateMenuRequest(input);
//
//        Account adminAccount = accountSetup.getAdminAccount();
//
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(adminAccount,
//                        null,adminAccount.getAuthorities()));
//
//        adminMenuAppService.update(category.getMenuId(),req);
//
//        SecurityContextHolder.getContext().setAuthentication(null);
//
//        em.flush(); em.clear();
//
//        PostRequest.UpdatePostRequest request = new PostRequest.UpdatePostRequest();
//        request.setTitle(UUID.randomUUID().toString());
//        request.setContents(UUID.randomUUID().toString());
//        request.setCategoryId(category.getMenuId());
//        request.setPined(false);
//        request.setExposeOption(new ExposeOptionRequest("PRIVACY","1234"));
//
//        mvc.perform(put(url + post.getPostId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .header("Authorization",tokenSetup.getAccessToken(formAccount))
//                        .content(objectMapper.writeValueAsString(request))
//                        .characterEncoding("UTF-8")
//                ).andDo(print())
//                .andExpect(status().isForbidden());
//    }
//    @Test
//    public void 게시글_삭제() throws Exception {
//
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//
//        Post post = postSetup.createPost(member, category);
//
//
//        em.flush(); em.clear();
//
//        mvc.perform(delete(url + post.getPostId())
//                .accept(MediaType.APPLICATION_JSON)
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",tokenSetup.getAccessToken(formAccount))
//        ).andDo(print())
//                .andExpect(status().isOk());
//
//        em.flush(); em.clear();
//
//        Post deletePost = postService.findById(post.getPostId());
//
//        Assertions.assertEquals(deletePost.getStatus(), Status.PERMANENT_DELETED);
//    }
//    @Test
//    public void 게시글_삭제_작성자도_권한도_없음() throws Exception {
//
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        Post post = postSetup.createPost(member, category);
//
//        MenuDTO.MenuAuthOption manage = new MenuDTO.MenuAuthOption();
//        manage.setOption(SelectOption.SELECT.getName());
//        manage.setRoles(List.of(roleSetup.createRole().getId()));
//
//        MenuDTO.MenuAuthOption[] input = new MenuDTO.MenuAuthOption[] {null,null,manage,manage};
//        MenuDTO.UpdateMenuRequest req = MenuRequestBuilder.getUpdateMenuRequest(input);
//
//        Account adminAccount = accountSetup.getAdminAccount();
//
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(adminAccount,
//                        null,adminAccount.getAuthorities()));
//
//        adminMenuAppService.update(category.getMenuId(),req);
//
//        SecurityContextHolder.getContext().setAuthentication(null);
//
//        em.flush(); em.clear();
//
//        mvc.perform(delete(url + post.getPostId())
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .header("Authorization",tokenSetup.getAccessToken(adminAccount))
//                ).andDo(print())
//                .andExpect(status().isForbidden());
//    }
//    @Test
//    public void 게시글_삭제_권한_있음() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        Post post = postSetup.createPost(member, category);
//
//        MenuDTO.MenuAuthOption manage = new MenuDTO.MenuAuthOption();
//        manage.setOption(SelectOption.SELECT.getName());
//        manage.setRoles(List.of(roleSetup.getRoleAdmin().getId()));
//
//        MenuDTO.MenuAuthOption[] input = new MenuDTO.MenuAuthOption[] {null,null,manage,manage};
//        MenuDTO.UpdateMenuRequest req = MenuRequestBuilder.getUpdateMenuRequest(input);
//
//        Account adminAccount = accountSetup.getAdminAccount();
//
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(adminAccount,
//                        null,adminAccount.getAuthorities()));
//
//        adminMenuAppService.update(category.getMenuId(),req);
//
//        SecurityContextHolder.getContext().setAuthentication(null);
//
//        em.flush(); em.clear();
//
//        mvc.perform(delete(url + post.getPostId())
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .header("Authorization",tokenSetup.getAccessToken(adminAccount))
//                ).andDo(print())
//                .andExpect(status().isOk());
//
//        Post deletePost = postService.findById(post.getPostId());
//
//        Assertions.assertEquals(deletePost.getStatus(),Status.PERMANENT_DELETED);
//    }
//
//    @DisplayName("공개 게시글 댓글을 조회한다.")
//    @Test
//    public void 게시글_댓글_조회() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//
//        Post post = postSetup.createPost(member, category);
//
//        for (int i = 0; i < 4; i++) {
//            commentSetup.createComment(post,member);
//        }
//
//        em.flush(); em.clear();
//
//        mvc.perform(get(url + post.getPostId() + "/comments")
//                .accept(MediaType.APPLICATION_JSON)
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//        ).andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content.size()").value(4));
//    }
//
//
//    @DisplayName("비밀 게시글 댓글 조회시, 비밀 게시글의 비밀번호를 입력한다.")
//    @Test
//    void 비밀게시글_댓글_조회() throws Exception {
//        //given
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//
//        Post privacyPost = postSetup.createPost(member, category, ExposeState.PRIVACY, "1234");
//
//        for (int i = 0; i < 4; i++) {
//            commentSetup.createComment(privacyPost,member);
//        }
//
//        em.flush(); em.clear();
//
//        //when //then
//        mvc.perform(get(url + privacyPost.getPostId() + "/comments")
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .queryParam("password","1234")
//                ).andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content.size()").value(4));
//    }
//
//
//    @DisplayName("비밀 게시글 댓글 조회시, 자신이 작성한 게시글은 비밀번호 입력이 없어도 조회가능하다.")
//    @Test
//    void 비밀게시글_댓글_조회_자기_게시글_조회() throws Exception {
//        //given
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//
//        Post privacyPost = postSetup.createPost(member, category, ExposeState.PRIVACY, "1234");
//
//        for (int i = 0; i < 4; i++) {
//            commentSetup.createComment(privacyPost,member);
//        }
//
//        em.flush(); em.clear();
//
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(formAccount,
//                        null,formAccount.getAuthorities()));
//
//        //when //then
//        mvc.perform(get(url + privacyPost.getPostId() + "/comments")
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                ).andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content.size()").value(4));
//    }
//
//
//    @DisplayName("비밀 게시글 댓글 조회시, 비밀 게시글 작성자가 아닐때 비밀번호가 없으면 조회가 불가능하다.")
//    @Test
//    void 비밀게시글_댓글_비밀번호없을때_조회불가능() throws Exception {
//        //given
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//
//        Post privacyPost = postSetup.createPost(member, category, ExposeState.PRIVACY, "1234");
//
//        for (int i = 0; i < 4; i++) {
//            commentSetup.createComment(privacyPost,member);
//        }
//
//        em.flush(); em.clear();
//
//        FormAccount otherAccount = accountSetup.createRandomUserAccount();
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(otherAccount,
//                        null,otherAccount.getAuthorities()));
//
//        //when //then
//        mvc.perform(get(url + privacyPost.getPostId() + "/comments")
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                ).andDo(print())
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.code").value(114))
//                .andExpect(jsonPath("$.message").value("게시글 비밀번호가 일치하지 않습니다."));
//    }
}