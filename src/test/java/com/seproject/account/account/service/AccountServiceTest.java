package com.seproject.account.account.service;

import com.seproject.account.account.domain.Account;
import com.seproject.account.account.domain.FormAccount;
import com.seproject.account.account.domain.OAuthAccount;
import com.seproject.account.role.domain.Role;
import com.seproject.board.common.Status;
import com.seproject.global.IntegrationTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class AccountServiceTest extends IntegrationTestSupport {
//    @Test
//    public void 로그인_아이디_중복_테스트() throws Exception {
//        String loginId = UUID.randomUUID().toString();
//        Assertions.assertFalse(accountService.isExistLoginId(loginId));
//        accountService.createFormAccount(loginId, "name", UUID.randomUUID().toString(), List.of());
//        Assertions.assertTrue(accountService.isExistLoginId(loginId));
//
//        FormAccount permanentDeleteAccount =
//                accountSetup.createFormAccount(UUID.randomUUID().toString(), "name", List.of(), LocalDateTime.now(),Status.PERMANENT_DELETED);
//        Assertions.assertFalse(accountService.isExistLoginId(permanentDeleteAccount.getLoginId()));
//
//        FormAccount tempDeleteAccount =
//                accountSetup.createFormAccount(UUID.randomUUID().toString(), "name", List.of(), LocalDateTime.now(),Status.TEMP_DELETED);
//        Assertions.assertTrue(accountService.isExistLoginId(tempDeleteAccount.getLoginId()));
//    }
//
//    @Test
//    public void 소셜_로그인_사용자_테스트() {
//        FormAccount formAccount = accountSetup.createFormAccount("loginId", "name", List.of(), LocalDateTime.now(), Status.NORMAL);
//        Assertions.assertFalse(accountService.isOAuthUser(formAccount.getLoginId()));
//        OAuthAccount oAuthAccount = accountSetup.createOAuthAccount();
//        Assertions.assertTrue(accountService.isOAuthUser(oAuthAccount.getLoginId()));
//    }
//
//    @Test
//    public void 비밀번호_일치_테스트() throws Exception {
//        String password = "D337";
//        Long accountId = accountService.createAccount("loginId", password, "name", List.of());
//        Account account = accountService.findById(accountId);
//        Assertions.assertTrue(accountService.matchPassword(account, "D337"));
//        Assertions.assertFalse(accountService.matchPassword(account,"D336"));
//        Assertions.assertFalse(accountService.matchPassword(account,"D445"));
//    }
//
//    @Test
//    public void 계정_수정_테스트() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        String updateLoginId = "updateLoginId";
//        String updateName = "updateName";
//        String updateNickname = "updateNickname";
//        String newPassword = UUID.randomUUID().toString();
//        Role roleAdmin = roleSetup.getRoleAdmin();
//        accountService.updateAccount(formAccount.getAccountId(),updateLoginId,
//                newPassword,updateName,List.of(roleAdmin));
//
//        Assertions.assertEquals(formAccount.getLoginId(),updateLoginId);
//        Assertions.assertEquals(formAccount.getName(),updateName);
//        Assertions.assertEquals(formAccount.getRoleAccounts().size(),1);
//        Assertions.assertEquals(formAccount.getRoles().get(0),roleAdmin);
//
//        Assertions.assertTrue(accountService.matchPassword(formAccount,newPassword));
//    }
//
//    @Test
//    public void 소셜계정_삭제_테스트() throws Exception {
//        OAuthAccount oAuthAccount = accountSetup.createOAuthAccount();
//        accountService.deleteAccount(oAuthAccount.getAccountId());
//        Optional<OAuthAccount> optional = oAuthAccountRepository.findOAuthAccountBySubAndProvider(oAuthAccount.getSub(), oAuthAccount.getProvider());
//        Assertions.assertTrue(optional.isEmpty());
//
//        OAuthAccount findAccount = accountService.findOAuthAccountById(oAuthAccount.getAccountId());
//        Assertions.assertNull(findAccount.getSub());
//    }
//
//    @Test
//    public void 계정_삭제_테스트() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        accountService.deleteAccount(formAccount.getAccountId());
//        Account account = accountService.findById(formAccount.getAccountId());
//        Assertions.assertEquals(account.getStatus(),Status.PERMANENT_DELETED);
//    }
//
//    @Test
//    public void 비밀번호_reset_테스트() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        String newPassword = "newPassword";
//        accountService.changePassword(formAccount, newPassword);
//        Assertions.assertTrue(accountService.matchPassword(formAccount, newPassword));
//    }









}