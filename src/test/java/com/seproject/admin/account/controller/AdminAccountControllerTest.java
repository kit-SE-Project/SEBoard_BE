//package com.seproject.admin.account.controller;
//
//import com.seproject.account.account.domain.Account;
//import com.seproject.account.account.domain.FormAccount;
//import com.seproject.account.account.domain.OAuthAccount;
//import com.seproject.account.role.domain.Role;
//import com.seproject.board.comment.domain.model.Comment;
//import com.seproject.board.common.Status;
//import com.seproject.board.menu.domain.Category;
//import com.seproject.board.post.domain.model.Bookmark;
//import com.seproject.board.post.domain.model.Post;
//import com.seproject.error.errorCode.ErrorCode;
//import com.seproject.global.IntegrationTestSupport;
//import com.seproject.member.domain.Member;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.ResultActions;
//import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
//import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//import java.util.stream.Collectors;
//
//import static com.seproject.admin.account.controller.dto.AdminAccountDto.CreateAccountRequest;
//import static com.seproject.admin.account.controller.dto.AdminAccountDto.UpdateAccountRequest;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//class AdminAccountControllerTest extends IntegrationTestSupport {
//
//    @Test
//    public void 모든_계정_목록_조회() throws Exception {
//
//        for (int i = 0; i < 25; i++) {
//            FormAccount formAccount = accountSetup.createFormAccount();
//            Member member = boardUserSetup.createMember(formAccount);
//        }
//
//        em.flush();
//        em.clear();
//
//        int page = 1;
//        int perPage = 10;
//
//        ResultActions perform = mvc.perform(get("/admin/accounts")
//                .queryParam("page", String.valueOf(page))
//                .queryParam("perPage", String.valueOf(perPage))
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .accept(MediaType.APPLICATION_JSON)
//        );
//
//        perform.andDo(print())
//                .andExpect(status().isOk());
//
//
//        perform.andExpect(jsonPath("$.content.size()").value(10));
//        for (int i = 0; i < perPage; i++) {
//            perform.andExpect(jsonPath("$.content[" +i + "]").exists());
//        }
//
//        perform
//                .andExpect(jsonPath("$.empty").value(false))
//                .andExpect(jsonPath("$.first").value(false))
//                .andExpect(jsonPath("$.last").value(false))
//                .andExpect(jsonPath("$.number").value(1))
//                .andExpect(jsonPath("$.numberOfElements").value(10))
//                .andExpect(jsonPath("$.size").value(10));
//    }
//
//    @Test
//    public void 삭제_계정_목록_조회() throws Exception {
//
//        for (int i = 0; i < 4; i++) {
//            FormAccount formAccount = accountSetup.createFormAccount();
//            boardUserSetup.createMember(formAccount);
//            formAccount.delete(false);
//        }
//
//        em.flush();
//        em.clear();
//
//        int page = 0;
//        int perPage = 35;
//
//        ResultActions perform = mvc.perform(get("/admin/accounts")
//                .queryParam("page", String.valueOf(page))
//                .queryParam("perPage", String.valueOf(perPage))
//                .queryParam("status", "TEMP_DELETED")
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .accept(MediaType.APPLICATION_JSON)
//        );
//
//        perform.andDo(print())
//                .andExpect(status().isOk());
//
//
//        perform.andExpect(jsonPath("$.content.size()").value(4));
//        for (int i = 0; i < 4; i++) {
//            perform.andExpect(jsonPath("$.content[" +i + "]").exists());
//        }
//
//        perform
//                .andExpect(jsonPath("$.empty").value(false))
//                .andExpect(jsonPath("$.first").value(true))
//                .andExpect(jsonPath("$.last").value(true))
//                .andExpect(jsonPath("$.number").value(0))
//                .andExpect(jsonPath("$.numberOfElements").value(4))
//                .andExpect(jsonPath("$.size").value(35));
//    }
//
//    @Test
//    public void 계정_상세_조회_테스트() throws Exception {
//
//        OAuthAccount oAuthAccount = accountSetup.createOAuthAccount();
//
//        em.flush();
//        em.clear();
//
//        ResultActions perform = mvc.perform(get("/admin/accounts/{accountId}",oAuthAccount.getAccountId())
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .header("Authorization",accessToken)
//                .characterEncoding("UTF-8")
//        );
//
//        perform.andDo(print())
//                .andExpect(status().isOk());
//
//        List<Role> roles = oAuthAccount.getRoles();
//        perform
//                .andExpect(jsonPath("$.accountId").value(oAuthAccount.getAccountId()))
//                .andExpect(jsonPath("$.loginId").value(oAuthAccount.getLoginId()))
//                .andExpect(jsonPath("$.name").value(oAuthAccount.getName()))
//                .andExpect(jsonPath("$.roles.size()").value(roles.size()))
//                    .andExpect(jsonPath("$.roles[0].alias").value(roles.get(0).toString()));
//    }
//
//    @Test
//    public void 계정_상세_조회_존재하지_않는_계정() throws Exception {
//
//        ResultActions perform = mvc.perform(get("/admin/accounts/{accountId}",423423)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .header("Authorization",accessToken)
//                .characterEncoding("UTF-8")
//        );
//
//        perform.andDo(print())
//                .andExpect(status().isNotFound());
//
//        perform
//                .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.getCode()))
//                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()));
//    }
//
//    @Test
//    public void 계정_생성() throws Exception {
//        Role role = roleSetup.createRole();
//        CreateAccountRequest request = createAccountRequest(null,null,null,null,List.of(role.getAuthority()));
//
//        Assertions.assertFalse(accountService.isExistLoginId(request.getId()));
//
//        em.flush();
//        em.clear();
//
//        ResultActions perform = mvc.perform(post("/admin/accounts")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .content(objectMapper.writeValueAsString(request))
//        );
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isOk());
//
//        boolean existLoginId = accountService.isExistLoginId(request.getId());
//        Assertions.assertTrue(existLoginId);
//    }
//
//    @Test
//    public void 계정_생성_이미_존재하는_아이디() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        CreateAccountRequest request = createAccountRequest(formAccount.getLoginId(), null, null, null, List.of("ROLE_USER"));
//
//        em.flush();
//        em.clear();
//
//        ResultActions perform = mvc.perform(post("/admin/accounts")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .content(objectMapper.writeValueAsString(request))
//        );
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isBadRequest());
//
//        perform
//                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(ErrorCode.USER_ALREADY_EXIST.getCode()))
//                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(ErrorCode.USER_ALREADY_EXIST.getMessage()));
//
//    }
//
//
//
//    @Test
//    public void 계정_생성_금지_닉네임() throws Exception {
//        String bannedNickname = UUID.randomUUID().toString();
//        bannedNicknameService.createBannedNickname(bannedNickname);
//        CreateAccountRequest request = createAccountRequest(null, bannedNickname, null, null, List.of("ROLE_USER"));
//
//        em.flush();
//        em.clear();
//
//        ResultActions perform = mvc.perform(post("/admin/accounts")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .content(objectMapper.writeValueAsString(request))
//        );
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isBadRequest());
//
//        perform
//                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(ErrorCode.BANNED_NICKNAME.getCode()))
//                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(ErrorCode.BANNED_NICKNAME.getMessage()));
//
//    }
//
//    @Test
//    public void 계정_생성_금지_아이디() throws Exception {
//        String bannedId = UUID.randomUUID().toString();
//        bannedIdService.createBannedId(bannedId);
//        CreateAccountRequest request = createAccountRequest(bannedId + "@gmail.com", null, null, null, List.of("ROLE_USER"));
//
//        em.flush();
//        em.clear();
//
//        ResultActions perform = mvc.perform(post("/admin/accounts")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .content(objectMapper.writeValueAsString(request))
//        );
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isBadRequest());
//
//        perform
//                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(ErrorCode.BANNED_ID.getCode()))
//                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(ErrorCode.BANNED_ID.getMessage()));
//
//    }
//
//    @Test
//    public void 계정_생성_이미_존재하는_닉네임() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//        CreateAccountRequest request = createAccountRequest(null, member.getName(), null, null, List.of("ROLE_USER"));
//
//        em.flush();
//        em.clear();
//
//        ResultActions perform = mvc.perform(post("/admin/accounts")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .content(objectMapper.writeValueAsString(request))
//        );
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isBadRequest());
//
//        perform
//                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(ErrorCode.ALREADY_EXIST_NICKNAME.getCode()))
//                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(ErrorCode.ALREADY_EXIST_NICKNAME.getMessage()));
//
//    }
//
//    @Test
//    public void 계정_수정() throws Exception {
//        Role role = roleSetup.createRole();
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//        em.flush();
//        em.clear();
//
//        UpdateAccountRequest request = updateAccountRequest(null, member.getName(), null, null, List.of(role.getAuthority()));
//        ResultActions perform = mvc.perform(put("/admin/accounts/{accountId}", formAccount.getAccountId())
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .content(objectMapper.writeValueAsString(request)));
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isOk());
//
//        Account findAccount = accountService.findById(formAccount.getAccountId());
//        List<String> collect = findAccount.getRoles().stream()
//                .map(Role::getAuthority)
//                .collect(Collectors.toList());
//
//        Assertions.assertTrue(collect.contains(role.getAuthority()));
//        Assertions.assertEquals(findAccount.getLoginId(),request.getId());
//        Assertions.assertEquals(member.getName(),request.getNickname());
//        Assertions.assertEquals(findAccount.getName(),request.getName());
//        Assertions.assertTrue(accountService.matchPassword(findAccount,request.getPassword()));
//    }
//
//    @Test
//    public void 계정_수정_이미_존재하는_아이디() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        UpdateAccountRequest request = updateAccountRequest(formAccount.getLoginId(), null, null, null, List.of("ROLE_USER"));
//
//        em.flush();
//        em.clear();
//
//        ResultActions perform = mvc.perform(put("/admin/accounts/{accountId}" , formAccount.getAccountId())
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .content(objectMapper.writeValueAsString(request))
//        );
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isBadRequest());
//
//        perform
//                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(ErrorCode.USER_ALREADY_EXIST.getCode()))
//                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(ErrorCode.USER_ALREADY_EXIST.getMessage()));
//
//    }
//
//    @Test
//    public void 계정_수정_금지_닉네임() throws Exception {
//        String bannedNickname = UUID.randomUUID().toString();
//        bannedNicknameService.createBannedNickname(bannedNickname);
//        UpdateAccountRequest request = updateAccountRequest(null, bannedNickname, null, null, List.of("ROLE_USER"));
//
//        em.flush();
//        em.clear();
//
//        ResultActions perform = mvc.perform(post("/admin/accounts")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .content(objectMapper.writeValueAsString(request))
//        );
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isBadRequest());
//
//        perform
//                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(ErrorCode.BANNED_NICKNAME.getCode()))
//                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(ErrorCode.BANNED_NICKNAME.getMessage()));
//
//    }
//
//    @Test
//    public void 계정_수정_금지_아이디() throws Exception {
//        String bannedId = UUID.randomUUID().toString();
//        bannedIdService.createBannedId(bannedId);
//        UpdateAccountRequest request = updateAccountRequest(bannedId + "@gmail.com", null, null, null, List.of("ROLE_USER"));
//
//        em.flush();
//        em.clear();
//
//        ResultActions perform = mvc.perform(post("/admin/accounts")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .content(objectMapper.writeValueAsString(request))
//        );
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isBadRequest());
//
//        perform
//                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(ErrorCode.BANNED_ID.getCode()))
//                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(ErrorCode.BANNED_ID.getMessage()));
//
//    }
//
//    @Test
//    public void 계정_수정_이미_존재하는_닉네임() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//        UpdateAccountRequest request = updateAccountRequest(null, member.getName(), null, null, List.of("ROLE_USER"));
//
//        em.flush();
//        em.clear();
//
//        ResultActions perform = mvc.perform(post("/admin/accounts")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .content(objectMapper.writeValueAsString(request))
//        );
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isBadRequest());
//
//        perform
//                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(ErrorCode.ALREADY_EXIST_NICKNAME.getCode()))
//                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(ErrorCode.ALREADY_EXIST_NICKNAME.getMessage()));
//    }
//
//    @Test
//    public void 계정_삭제() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()),
//                UUID.randomUUID().toString().substring(0,8),
//                UUID.randomUUID().toString(),
//                UUID.randomUUID().toString().substring(0,8));
//
//        Post post = postSetup.createPost(member, category);
//        Comment comment = commentSetup.createComment(post, member);
//        Bookmark bookmark = bookmarkSetup.createBookmark(post, member);
//
//        em.flush();
//        em.clear();
//
//        ResultActions perform = mvc.perform(delete("/admin/accounts/{accountId}", formAccount.getAccountId())
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .header("Authorization",accessToken)
//                .characterEncoding("UTF-8"));
//
//        perform.andDo(print())
//                .andExpect(status().isOk());
//
//        em.flush();
//        em.clear();
//
//        Account findAccount = accountService.findById(formAccount.getAccountId());
//        Assertions.assertEquals(findAccount.getStatus(), Status.PERMANENT_DELETED);
//
//        boolean existLoginId = accountService.isExistLoginId(findAccount.getLoginId());
//        Assertions.assertFalse(existLoginId);
//
//        Optional<Post> postById = postRepository.findById(post.getPostId());
//        Optional<Comment> commentById = commentRepository.findById(comment.getCommentId());
//        Optional<Bookmark> bookmarkById = bookmarkRepository.findById(bookmark.getBookmarkId());
//
//        Assertions.assertEquals(postById.get().getStatus(),Status.PERMANENT_DELETED);
//        Assertions.assertEquals(commentById.get().getStatus(),Status.PERMANENT_DELETED);
//        Assertions.assertFalse(bookmarkById.isPresent());
//    }
//
//    @Test
//    public void 계정_삭제_존재하지_않음() throws Exception {
//
//        ResultActions perform = mvc.perform(delete("/admin/accounts/{accountId}", 33)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//        );
//
//        perform.andDo(print())
//                .andExpect(status().isNotFound());
//    }
//
//
//    private UpdateAccountRequest updateAccountRequest(String email,
//                                                      String nickname,
//                                                      String name,
//                                                      String password,
//                                                      List<String> roles) {
//        UpdateAccountRequest request = new UpdateAccountRequest();
//        request.setId(email == null ? UUID.randomUUID().toString() : email);
//        request.setName(name == null ? UUID.randomUUID().toString() : name);
//        request.setNickname(nickname == null ? UUID.randomUUID().toString() : nickname);
//        request.setPassword(password == null ? UUID.randomUUID().toString() : password);
//        request.setRoles(roles);
//
//        return request;
//    }
//
//    private CreateAccountRequest createAccountRequest(String email,
//                                                                      String nickname,
//                                                                      String name,
//                                                                      String password,
//                                                                      List<String> roles) {
//
//        CreateAccountRequest request = new CreateAccountRequest();
//        request.setId(email == null ? UUID.randomUUID().toString() : email);
//        request.setNickname(nickname == null ? UUID.randomUUID().toString() : nickname);
//        request.setName(name == null ? UUID.randomUUID().toString() : name);
//        request.setPassword(password == null ? UUID.randomUUID().toString() : password);
//        request.setRoles(roles);
//
//        return request;
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
//}