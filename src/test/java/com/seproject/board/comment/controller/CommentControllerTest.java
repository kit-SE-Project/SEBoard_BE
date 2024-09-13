package com.seproject.board.comment.controller;

import com.seproject.global.IntegrationTestSupport;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;


class CommentControllerTest extends IntegrationTestSupport {
//
//
//    static final String url = "/comments/";
//
//    @Test
//    public void Named_댓글_작성() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        Member member = boardUserSetup.createMember(formAccount);
//        Post post = postSetup.createPost(member, category);
//
//        CreateCommentRequest request = new CreateCommentRequest();
//        request.setPostId(post.getPostId());
//        request.setContents(UUID.randomUUID().toString());
//        request.setReadOnlyAuthor(false);
//        request.setAnonymous(false);
//
//        String accessToken = tokenSetup.getAccessToken(formAccount);
//
//        em.flush(); em.clear();
//        mvc.perform(
//                post(url)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .header("Authorization",accessToken)
//                        .content(objectMapper.writeValueAsString(request))
//                        .characterEncoding("UTF-8")
//        ).andDo(print())
//                .andExpect(status().isCreated());
//
//        em.flush(); em.clear();
//
//        List<Comment> byPostId =
//                commentService.findWithAuthorByPostId(post.getPostId());
//
//        assertEquals(byPostId.size(),1);
//
//        Comment comment = byPostId.get(0);
//
//        assertEquals(comment.getContents(),request.getContents());
//        BoardUser author = comment.getAuthor();
//        assertEquals(author.getLoginId(),member.getLoginId());
//        assertEquals(author.getName(),member.getName());
//        assertFalse(author.isAnonymous());
//    }
//    @Test
//    public void Unnamed_댓글_작성() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        Member member = boardUserSetup.createMember(formAccount);
//        Post post = postSetup.createPost(member, category);
//
//        CreateCommentRequest request = new CreateCommentRequest();
//        request.setPostId(post.getPostId());
//        request.setContents(UUID.randomUUID().toString());
//        request.setReadOnlyAuthor(false);
//        request.setAnonymous(true);
//
//        String accessToken = tokenSetup.getAccessToken(formAccount);
//
//        em.flush(); em.clear();
//        mvc.perform(
//                        post(url)
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .accept(MediaType.APPLICATION_JSON)
//                                .header("Authorization",accessToken)
//                                .content(objectMapper.writeValueAsString(request))
//                                .characterEncoding("UTF-8")
//                ).andDo(print())
//                .andExpect(status().isCreated());
//
//        em.flush(); em.clear();
//
//        List<Comment> byPostId =
//                commentService.findWithAuthorByPostId(post.getPostId());
//
//        assertEquals(byPostId.size(),1);
//
//        Comment comment = byPostId.get(0);
//
//        assertEquals(comment.getContents(),request.getContents());
//        BoardUser author = comment.getAuthor();
//        assertEquals(author.getName(),"익명1");
//        assertTrue(author.isAnonymous());
//    }
//    @Test
//    public void 댓글_작성_권한_없음() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Menu root = menuSetup.createMenu();
//        BoardMenu boardMenu = menuSetup.createBoardMenu(root);
//        Category category = menuSetup.createCategory(boardMenu);
//        Member member = boardUserSetup.createMember(formAccount);
//        Post post = postSetup.createPost(member, category);
//
//        CreateCommentRequest request = new CreateCommentRequest();
//        request.setPostId(post.getPostId());
//        request.setContents(UUID.randomUUID().toString());
//        request.setReadOnlyAuthor(false);
//        request.setAnonymous(true);
//
//        MenuDTO.MenuAuthOption access = new MenuDTO.MenuAuthOption();
//        access.setOption(SelectOption.SELECT.getName());
//        access.setRoles(List.of(roleSetup.createRole().getId()));
//
//        MenuDTO.MenuAuthOption[] input = new MenuDTO.MenuAuthOption[] {access,access,null,null};
//        MenuDTO.UpdateMenuRequest req = MenuRequestBuilder.getUpdateMenuRequest(input);
//
//        Account adminAccount = accountSetup.getAdminAccount();
//
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(adminAccount,
//                        null,adminAccount.getAuthorities()));
//
//        adminMenuAppService.update(root.getMenuId(),req);
//
//        String accessToken = tokenSetup.getAccessToken(formAccount);
//
//        em.flush(); em.clear();
//        mvc.perform(
//                        post(url)
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .accept(MediaType.APPLICATION_JSON)
//                                .header("Authorization",accessToken)
//                                .content(objectMapper.writeValueAsString(request))
//                                .characterEncoding("UTF-8")
//                ).andDo(print())
//                .andExpect(status().isForbidden());
//    }
//    @Test
//    public void 내댓글_수정() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        Anonymous anonymous = boardUserSetup.createAnonymous(formAccount);
//        Post post = postSetup.createPost(anonymous, category);
//
//        Comment comment = commentSetup.createComment(post, anonymous);
//
//        UpdateCommentRequest request = new UpdateCommentRequest();
//        request.setContents(UUID.randomUUID().toString());
//        request.setReadOnlyAuthor(true);
//
//        mvc.perform(
//                put(url + "{commentId}",comment.getCommentId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .header("Authorization", tokenSetup.getAccessToken(formAccount))
//                        .content(objectMapper.writeValueAsString(request))
//                        .characterEncoding("UTF-8")
//        ).andDo(print())
//                .andExpect(status().isOk());
//
//        Comment findComment = commentService.findById(comment.getCommentId());
//
//        assertEquals(findComment.getContents(),request.getContents());
//        assertEquals(findComment.isOnlyReadByAuthor(),request.isReadOnlyAuthor());
//    }
//    @Test
//    public void 댓글_수정_권한이_없을때() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Menu root = menuSetup.createMenu();
//        BoardMenu boardMenu = menuSetup.createBoardMenu(root);
//        Category category = menuSetup.createCategory(boardMenu);
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
//
//        Account anotherAccount = accountSetup.createFormAccount();
//        String accessToken = tokenSetup.getAccessToken(anotherAccount);
//
//        Member member = boardUserSetup.createMember(formAccount);
//        Post post = postSetup.createPost(member, category);
//        Comment comment = commentSetup.createComment(post, member);
//
//        UpdateCommentRequest request = new UpdateCommentRequest();
//        request.setContents(UUID.randomUUID().toString());
//        request.setReadOnlyAuthor(true);
//
//        em.flush(); em.clear();
//        mvc.perform(
//                        put(url + "{commentId}",comment.getCommentId())
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .accept(MediaType.APPLICATION_JSON)
//                                .header("Authorization",accessToken)
//                                .content(objectMapper.writeValueAsString(request))
//                                .characterEncoding("UTF-8")
//                ).andDo(print())
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    public void 내댓글_삭제() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        Anonymous anonymous = boardUserSetup.createAnonymous(formAccount);
//        Post post = postSetup.createPost(anonymous, category);
//
//        Comment comment = commentSetup.createComment(post, anonymous);
//
//        em.flush(); em.clear();
//
//        String accessToken = tokenSetup.getAccessToken(formAccount);
//        mvc.perform(
//                delete(url + "{commentId}",comment.getCommentId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .header("Authorization",accessToken)
//        ).andDo(print())
//                .andExpect(status().isOk());
//
//        em.flush(); em.clear();
//
//        Comment findComment = commentService.findById(comment.getCommentId());
//        assertEquals(findComment.getStatus(), Status.PERMANENT_DELETED);
//    }
//
//    @Test
//    public void 댓글_삭제_권한이_없을때() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Menu root = menuSetup.createMenu();
//        BoardMenu boardMenu = menuSetup.createBoardMenu(root);
//        Category category = menuSetup.createCategory(boardMenu);
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
//        Account anotherAccount = accountSetup.createFormAccount();
//        String accessToken = tokenSetup.getAccessToken(anotherAccount);
//
//        Member member = boardUserSetup.createMember(formAccount);
//        Post post = postSetup.createPost(member, category);
//        Comment comment = commentSetup.createComment(post, member);
//
//        em.flush(); em.clear();
//        mvc.perform(
//                        delete(url + "{commentId}",comment.getCommentId())
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .accept(MediaType.APPLICATION_JSON)
//                                .header("Authorization",accessToken)
//                                .characterEncoding("UTF-8")
//                ).andDo(print())
//                .andExpect(status().isForbidden());
//    }
//
//
//
}