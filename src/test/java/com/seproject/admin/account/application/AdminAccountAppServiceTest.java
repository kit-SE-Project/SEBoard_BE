package com.seproject.admin.account.application;

import com.seproject.global.IntegrationTestSupport;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminAccountAppServiceTest extends IntegrationTestSupport {
//
//    @BeforeEach
//    public void set() {
//        Account adminAccount = accountSetup.getAdminAccount();
//        UsernamePasswordAuthenticationToken authentication =
//                new UsernamePasswordAuthenticationToken(adminAccount, null, adminAccount.getAuthorities());
//        SecurityContextHolder.getContext().setAuthentication(authentication);
//    }
//
//    @Test
//    public void 목록_조회_테스트() throws Exception {
//        for (int i = 0; i < 19; i++) {
//            FormAccount formAccount = accountSetup.createFormAccount();
//            Member member = boardUserSetup.createMember(formAccount);
//        }
//
//        em.flush(); em.clear();
//
//        Page<AccountResponse> allAccount =
//                accountAppService.findAllAccount(new AccountCondition(), 1, 10);
//        Assertions.assertEquals(allAccount.getContent().size(),9);
//    }
//
//    @Test
//    public void 계정_상세_조회() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//
//        Long accountId = formAccount.getAccountId();
//        AccountResponse accountResponse = accountAppService.findAccount(accountId);
//
//        Assertions.assertEquals(accountResponse.getAccountId(),accountId);
//        Assertions.assertEquals(accountResponse.getName(),formAccount.getName());
//        Assertions.assertEquals(accountResponse.getRegisteredDate(),formAccount.getCreatedAt());
//
//        List<String> responseRoles = accountResponse.getRoles()
//                .stream().map(RoleDTO.RoleResponse::getAlias)
//                .collect(Collectors.toList());
//        List<String> collect = formAccount.getRoles().stream()
//                .map(Role::toString)
//                .collect(Collectors.toList());
//
//        Assertions.assertEquals(responseRoles, collect);
//    }
//
//    @Test
//    public void 계정_상세_조회_존재하지_않음() throws Exception {
//        CustomIllegalArgumentException customIllegalArgumentException = assertThrows(CustomIllegalArgumentException.class, () -> {
//            accountAppService.findAccount(1234123L);
//        });
//
//        Assertions.assertEquals(customIllegalArgumentException.getErrorCode(), ErrorCode.USER_NOT_FOUND);
//    }
//
//    @Test
//    public void 관리자_계정_생성_테스트() throws Exception {
//        Role role = roleSetup.createRole();
//        CreateAccountRequest request = createAccountRequest(null, null, null, null, List.of(role.getAuthority()));
//        Long accountId = accountAppService.createAccount(request);
//
//        Account findAccount = accountService.findById(accountId);
//
//        Assertions.assertEquals(request.getId(),findAccount.getLoginId());
//        Assertions.assertEquals(request.getName(),findAccount.getName());
//
//        Assertions.assertTrue(passwordEncoder.matches(request.getPassword(),findAccount.getPassword()));
//        Assertions.assertTrue(findAccount.getRoles().contains(role));
//    }
//
//    @Test
//    public void 관리자_계정_생성_이미_존재하는_아이디() throws Exception {
//        Role role = roleSetup.createRole();
//        FormAccount formAccount = accountSetup.createFormAccount();
//        CreateAccountRequest request = createAccountRequest(formAccount.getLoginId(), null, null, null, List.of(role.getAuthority()));
//
//
//        CustomIllegalArgumentException customIllegalArgumentException = assertThrows(CustomIllegalArgumentException.class, () -> {
//            Long accountId = accountAppService.createAccount(request);
//        });
//
//        ErrorCode errorCode = customIllegalArgumentException.getErrorCode();
//        Assertions.assertEquals(errorCode,ErrorCode.USER_ALREADY_EXIST);
//    }
//
//    @Test
//    public void 관리자_계정_생성_금지_아이디() throws Exception {
//        String bannedId = UUID.randomUUID().toString();
//        bannedIdService.createBannedId(bannedId);
//        Role role = roleSetup.createRole();
//        CreateAccountRequest request = createAccountRequest(bannedId, null, null, null, List.of(role.getAuthority()));
//
//        CustomIllegalArgumentException customIllegalArgumentException = assertThrows(CustomIllegalArgumentException.class, () -> {
//            Long accountId = accountAppService.createAccount(request);
//        });
//
//        ErrorCode errorCode = customIllegalArgumentException.getErrorCode();
//        Assertions.assertEquals(errorCode,ErrorCode.BANNED_ID);
//    }
//
//    @Test
//    public void 관리자_계정_생성_금지_닉네임() throws Exception {
//        String bannedNickname = UUID.randomUUID().toString();
//        bannedNicknameService.createBannedNickname(bannedNickname);
//        Role role = roleSetup.createRole();
//        CreateAccountRequest request = createAccountRequest(null, bannedNickname, null, null, List.of(role.getAuthority()));
//
//        CustomIllegalArgumentException customIllegalArgumentException = assertThrows(CustomIllegalArgumentException.class, () -> {
//            Long accountId = accountAppService.createAccount(request);
//        });
//
//        ErrorCode errorCode = customIllegalArgumentException.getErrorCode();
//        Assertions.assertEquals(errorCode,ErrorCode.BANNED_NICKNAME);
//    }
//    @Test
//    public void 관리자_계정_수정() throws Exception {
//        OAuthAccount oAuthAccount = accountSetup.createOAuthAccount();
//        Member member = boardUserSetup.createMember(oAuthAccount);
//        UpdateAccountRequest request = updateAccountRequest(null,null,null,null,List.of("ROLE_USER", "ROLE_KUMOH"));
//        accountAppService.updateAccount(oAuthAccount.getAccountId(), request);
//
//        em.flush();
//        em.clear();
//
//        Account findAccount = accountService.findById(oAuthAccount.getAccountId());
//        Member findMember = memberService.findByAccountId(findAccount.getAccountId());
//
//        Assertions.assertEquals(request.getName(),findAccount.getName());
//        Assertions.assertEquals(request.getNickname(),findMember.getName());
//        Assertions.assertEquals(request.getId(),findAccount.getLoginId());
//
//        Assertions.assertTrue(passwordEncoder.matches(request.getPassword(),findAccount.getPassword()));
//        List<String> collect = findAccount.getRoles().stream()
//                .map(Role::getAuthority)
//                .collect(Collectors.toList());
////        List<String> roles = request.getRoles();
////        for (int i = 0; i < collect.size(); i++) {
////            String role = collect.get(i);
////            Assertions.assertTrue(roles.contains(role));
////        }
//
//    }
//
//    @Test
//    public void 관리자_계정_수정_이미_존재하는_아이디() throws Exception {
//        Role role = roleSetup.createRole();
//        FormAccount formAccount = accountSetup.createFormAccount();
//        UpdateAccountRequest request = updateAccountRequest(formAccount.getLoginId(), null, null, null, List.of(role.getAuthority()));
//
//        CustomIllegalArgumentException customIllegalArgumentException = assertThrows(CustomIllegalArgumentException.class, () -> {
//            accountAppService.updateAccount(formAccount.getAccountId(),request);
//        });
//
//        ErrorCode errorCode = customIllegalArgumentException.getErrorCode();
//        Assertions.assertEquals(errorCode,ErrorCode.USER_ALREADY_EXIST);
//    }
//
//    @Test
//    public void 관리자_계정_수정_금지_아이디() throws Exception {
//        String bannedId = UUID.randomUUID().toString();
//        bannedIdService.createBannedId(bannedId);
//        Role role = roleSetup.createRole();
//
//        OAuthAccount oAuthAccount = accountSetup.createOAuthAccount();
//
//        UpdateAccountRequest request = updateAccountRequest(bannedId, null, null, null, List.of(role.getAuthority()));
//        CustomIllegalArgumentException customIllegalArgumentException = assertThrows(CustomIllegalArgumentException.class, () -> {
//            accountAppService.updateAccount(oAuthAccount.getAccountId(), request);
//        });
//
//        ErrorCode errorCode = customIllegalArgumentException.getErrorCode();
//        Assertions.assertEquals(errorCode,ErrorCode.BANNED_ID);
//    }
//
//    @Test
//    public void 관리자_계정_수정_금지_닉네임() throws Exception {
//        String bannedNickname = UUID.randomUUID().toString();
//        bannedNicknameService.createBannedNickname(bannedNickname);
//        OAuthAccount oAuthAccount = accountSetup.createOAuthAccount();
//        UpdateAccountRequest request = updateAccountRequest(null, bannedNickname, null, null, List.of());
//
//        CustomIllegalArgumentException customIllegalArgumentException = assertThrows(CustomIllegalArgumentException.class, () -> {
//            accountAppService.updateAccount(oAuthAccount.getAccountId(),request);
//        });
//
//        ErrorCode errorCode = customIllegalArgumentException.getErrorCode();
//        Assertions.assertEquals(errorCode,ErrorCode.BANNED_NICKNAME);
//    }
//
//    @Test
//    public void 관리자_계정_삭제_테스트() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        Post post = postSetup.createPost(member, category);
//        Comment comment = commentSetup.createComment(post, member);
//        Bookmark bookmark = bookmarkSetup.createBookmark(post, member);
//
//        em.flush();
//        em.clear();
//
////        accountAppService.deleteAccount(formAccount.getAccountId());
//
//        em.flush();
//        em.clear();
//
//        boolean existLoginId = accountService.isExistLoginId(formAccount.getLoginId());
//        Assertions.assertFalse(existLoginId);
//
//        Optional<Post> postById = postRepository.findById(post.getPostId());
//        Optional<Comment> commentById = commentRepository.findById(comment.getCommentId());
//        Optional<Bookmark> bookmarkById = bookmarkRepository.findById(bookmark.getBookmarkId());
//
//        Assertions.assertEquals(postById.get().getStatus(), Status.PERMANENT_DELETED);
//        Assertions.assertEquals(commentById.get().getStatus(),Status.PERMANENT_DELETED);
//        Assertions.assertFalse(bookmarkById.isPresent());
//    }
//
//
//    @Test
//    public void 계정_벌크_삭제() throws Exception {
//        List<Account> accounts = new ArrayList<>();
//        for (int i = 0; i < 20; i++) {
//            accounts.add(accountSetup.createFormAccount());
//        }
//
//        List<Long> collect = accounts.stream()
//                .map(Account::getAccountId)
//                .collect(Collectors.toList());
//
////        accountAppService.deleteAccount(collect);
//        em.flush();
//        em.clear();
//
//        for (Account account : accounts) {
//            boolean existLoginId = accountService.isExistLoginId(account.getLoginId());
//            Assertions.assertFalse(existLoginId);
//        }
//    }
//
//
//
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
////        request.setRoles(roles);
//
//        return request;
//    }
//
//    private CreateAccountRequest createAccountRequest(String email,
//                                                      String nickname,
//                                                      String name,
//                                                      String password,
//                                                      List<String> roles) {
//
//        CreateAccountRequest request = new CreateAccountRequest();
//        request.setId(email == null ? UUID.randomUUID().toString() : email);
//        request.setNickname(nickname == null ? UUID.randomUUID().toString() : nickname);
//        request.setName(name == null ? UUID.randomUUID().toString() : name);
//        request.setPassword(password == null ? UUID.randomUUID().toString() : password);
////        request.setRoles(roles);
//
//        return request;
//    }
}