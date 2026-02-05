package com.seproject.account.account.controller;

import com.seproject.account.account.controller.dto.KumohAuthDTO;
import com.seproject.account.account.controller.dto.MyPageDTO;
import com.seproject.account.account.domain.Account;
import com.seproject.account.account.domain.FormAccount;
import com.seproject.account.account.domain.OAuthAccount;
import com.seproject.account.email.domain.KumohConfirmedEmail;
import com.seproject.account.email.domain.PasswordChangeConfirmedEmail;
import com.seproject.account.role.domain.Role;
import com.seproject.account.token.domain.JWT;
import com.seproject.board.common.Status;
import com.seproject.global.IntegrationTestSupport;
import com.seproject.global.data_setup.BoardUserSetup;
import com.seproject.member.domain.Member;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.seproject.account.account.controller.dto.LogoutDTO.LogoutRequestDTO;
import static com.seproject.account.account.controller.dto.PasswordDTO.PasswordChangeRequest;
import static com.seproject.account.account.controller.dto.PasswordDTO.ResetPasswordRequest;
import static com.seproject.account.account.controller.dto.WithDrawDTO.WithDrawRequest;

class AccountControllerTest extends IntegrationTestSupport {
//    @Test
//    public void 로그아웃_테스트_성공() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//
//        UsernamePasswordAuthenticationToken token
//                = new UsernamePasswordAuthenticationToken(formAccount.getLoginId(),formAccount.getPassword(),formAccount.getAuthorities());
//        JWT accessToken = tokenService.createAccessToken(token);
//        JWT refreshToken = tokenService.createRefreshToken(token);
//
//        LogoutRequestDTO requestDTO = new LogoutRequestDTO();
//        requestDTO.setRefreshToken(refreshToken.getToken());
//
//        em.flush();
//        em.clear();
//
//        ResultActions perform = mvc.perform(
//                MockMvcRequestBuilders.post("/logoutProc")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .accept(MediaType.APPLICATION_JSON)
//                        .header(HttpHeaders.AUTHORIZATION,accessToken.getToken())
//                        .content(objectMapper.writeValueAsString(requestDTO))
//        );
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isOk());
//
//        Assertions.assertTrue(logoutTokenRepository.existsById(accessToken.getToken()));
//        Assertions.assertTrue(logoutRefreshTokenRepository.existsById(refreshToken.getToken()));
//    }
//
//    @Test
//    public void 로그아웃_테스트_로그인_상태가_아님() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//
//        UsernamePasswordAuthenticationToken token
//                = new UsernamePasswordAuthenticationToken(formAccount.getLoginId(),formAccount.getPassword(),formAccount.getAuthorities());
//
//        JWT refreshToken = tokenService.createRefreshToken(token);
//
//        SecurityContextHolder.getContext().setAuthentication(token);
//        LogoutRequestDTO requestDTO = new LogoutRequestDTO();
//        requestDTO.setRefreshToken(refreshToken.getToken());
//
//        em.flush();
//        em.clear();
//
//        ResultActions perform = mvc.perform(
//                MockMvcRequestBuilders.post("/logoutProc")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .accept(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDTO))
//        );
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
//    }
//
//    @Test
//    public void 로그아웃_테스트_이상한_토큰을_전달() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//
//        UsernamePasswordAuthenticationToken token
//                = new UsernamePasswordAuthenticationToken(formAccount.getLoginId(),formAccount.getPassword(),formAccount.getAuthorities());
//        JWT accessToken = tokenService.createAccessToken(token);
//
//        LogoutRequestDTO requestDTO = new LogoutRequestDTO();
//        requestDTO.setRefreshToken(UUID.randomUUID().toString());
//
//        em.flush();
//        em.clear();
//
//        ResultActions perform = mvc.perform(
//                MockMvcRequestBuilders.post("/logoutProc")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .accept(MediaType.APPLICATION_JSON)
//                        .header(HttpHeaders.AUTHORIZATION,accessToken.getToken())
//                        .content(objectMapper.writeValueAsString(requestDTO))
//        );
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
//    }
//
//    @Test
//    public void 소셜_로그아웃_테스트_성공() throws Exception {
//        OAuthAccount oAuthAccount = accountSetup.createOAuthAccount();
//
//        UsernamePasswordAuthenticationToken token
//                = new UsernamePasswordAuthenticationToken(oAuthAccount.getLoginId(),oAuthAccount.getPassword(),oAuthAccount.getAuthorities());
//        JWT accessToken = tokenService.createAccessToken(token);
//        JWT refreshToken = tokenService.createRefreshToken(token);
//
//        LogoutRequestDTO requestDTO = new LogoutRequestDTO();
//        requestDTO.setRefreshToken(refreshToken.getToken());
//
//        em.flush();
//        em.clear();
//
//        ResultActions perform = mvc.perform(
//                MockMvcRequestBuilders.post("/logoutProc")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .accept(MediaType.APPLICATION_JSON)
//                        .header(HttpHeaders.AUTHORIZATION,accessToken.getToken())
//                        .content(objectMapper.writeValueAsString(requestDTO)));
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isOk())
//                .andExpect(MockMvcResultMatchers.jsonPath("$.requiredRedirect").value(true))
//                .andExpect(MockMvcResultMatchers.jsonPath("$.url").isNotEmpty());
//
//        Assertions.assertTrue(logoutTokenRepository.existsById(accessToken.getToken()));
//        Assertions.assertTrue(logoutRefreshTokenRepository.existsById(refreshToken.getToken()));
//    }
//
//    @Test
//    public void 회원_탈퇴_성공() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        UsernamePasswordAuthenticationToken token
//                = new UsernamePasswordAuthenticationToken(formAccount.getLoginId(),formAccount.getPassword(),formAccount.getAuthorities());
//        JWT accessToken = tokenService.createAccessToken(token);
//        JWT refreshToken = tokenService.createRefreshToken(token);
//
//        WithDrawRequest request = new WithDrawRequest();
//        request.setRefreshToken(refreshToken.getToken());
//
//        ResultActions perform = mvc.perform(
//                MockMvcRequestBuilders.delete("/withdraw")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .accept(MediaType.APPLICATION_JSON)
//                        .header(HttpHeaders.AUTHORIZATION, accessToken.getToken())
//                        .content(objectMapper.writeValueAsString(request)));
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isOk())
//                .andExpect(MockMvcResultMatchers.jsonPath("$.requiredRedirect").value(false))
//                .andExpect(MockMvcResultMatchers.jsonPath("$.accountId").value(formAccount.getAccountId()))
//                .andExpect(MockMvcResultMatchers.jsonPath("$.loginId").value(formAccount.getLoginId()))
//                .andExpect(MockMvcResultMatchers.jsonPath("$.roles.size()").value(1))
//                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(formAccount.getName()));
//
//        Account account = accountService.findById(formAccount.getAccountId());
//        Assertions.assertEquals(account.getStatus(), Status.PERMANENT_DELETED);
//    }
//
//    @Test
//    public void 회원_탈퇴_로그인_상태가_아님() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        UsernamePasswordAuthenticationToken token
//                = new UsernamePasswordAuthenticationToken(formAccount.getLoginId(),formAccount.getPassword(),formAccount.getAuthorities());
//        JWT refreshToken = tokenService.createRefreshToken(token);
//
//        WithDrawRequest request = new WithDrawRequest();
//        request.setRefreshToken(refreshToken.getToken());
//
//        ResultActions perform = mvc.perform(
//                MockMvcRequestBuilders.delete("/withdraw")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .accept(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)));
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
//    }
//
//    @Test
//    public void 비밀번호_찾기() throws Exception {
//        String email = UUID.randomUUID().toString() + "@kumoh.ac.kr";
//        FormAccount formAccount = accountSetup.createFormAccount(email, "name", List.of(), LocalDateTime.now(),Status.NORMAL);
//
//        ResetPasswordRequest request = new ResetPasswordRequest();
//        String newPassword = UUID.randomUUID().toString();
//        request.setEmail(email);
//        request.setPassword(newPassword);
//
//        passwordChangeConfirmedEmailRepository.save(new PasswordChangeConfirmedEmail(email));
//
//        ResultActions perform = mvc.perform(
//                MockMvcRequestBuilders.post("/password")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF=8")
//                        .accept(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)));
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isOk());
//
//        Account account = accountService.findById(formAccount.getAccountId());
//
//        Assertions.assertTrue(accountService.matchPassword(account,newPassword));
//    }
//
//    @Test
//    public void 비밀번호_찾기_미인증_이메일() throws Exception {
//        String email = UUID.randomUUID().toString() + "@kumoh.ac.kr";
//        FormAccount formAccount = accountSetup.createFormAccount(email, "name", List.of(), LocalDateTime.now(),Status.NORMAL);
//
//        ResetPasswordRequest request = new ResetPasswordRequest();
//        String newPassword = UUID.randomUUID().toString();
//        request.setEmail(email);
//        request.setPassword(newPassword);
//
//        ResultActions perform = mvc.perform(
//                MockMvcRequestBuilders.post("/password")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF=8")
//                        .accept(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)));
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isNotFound());
//    }
//
//    @Test
//    public void 금오인_인증() throws Exception {
//        Role role = roleSetup.getRoleUser();
//        ArrayList<Role> roles = new ArrayList<>(List.of(role));
//        String email = UUID.randomUUID().toString() + "@kumoh.ac.kr";
//        FormAccount formAccount = accountSetup.createFormAccount(email, "name", roles, LocalDateTime.now(), Status.NORMAL);
//        KumohAuthDTO.KumohAuthRequest request = new KumohAuthDTO.KumohAuthRequest();
//        request.setEmail(email);
//        kumohConfirmedEmailRepository.save(new KumohConfirmedEmail(email));
//
//        UsernamePasswordAuthenticationToken token =
//                new UsernamePasswordAuthenticationToken(email, formAccount.getPassword(), formAccount.getAuthorities());
//        JWT accessToken = tokenService.createAccessToken(token);
//
//        ResultActions perform = mvc.perform(MockMvcRequestBuilders.post("/kumoh")
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .accept(MediaType.APPLICATION_JSON)
//                .header(HttpHeaders.AUTHORIZATION,accessToken.getToken())
//                .content(objectMapper.writeValueAsString(request))
//        );
//
//
//        Account account = accountService.findById(formAccount.getAccountId());
//        Role roleKumoh = roleSetup.getRoleKumoh();
//        Assertions.assertTrue(account.getRoles().contains(roleKumoh));
//    }
//
//    @Test
//    public void 금오인_인증_미인증_이메일() throws Exception {
//        Role role = roleSetup.getRoleUser();
//        ArrayList<Role> roles = new ArrayList<>(List.of(role));
//        String email = UUID.randomUUID().toString() + "@kumoh.ac.kr";
//        FormAccount formAccount = accountSetup.createFormAccount(email, "name", roles, LocalDateTime.now(), Status.NORMAL);
//        KumohAuthDTO.KumohAuthRequest request = new KumohAuthDTO.KumohAuthRequest();
//        request.setEmail(email);
//
//        UsernamePasswordAuthenticationToken token =
//                new UsernamePasswordAuthenticationToken(email, formAccount.getPassword(), formAccount.getAuthorities());
//        JWT accessToken = tokenService.createAccessToken(token);
//
//        ResultActions perform = mvc.perform(MockMvcRequestBuilders.post("/kumoh")
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .accept(MediaType.APPLICATION_JSON)
//                .header(HttpHeaders.AUTHORIZATION,accessToken.getToken())
//                .content(objectMapper.writeValueAsString(request))
//        );
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isNotFound());
//    }
//    @Autowired BoardUserSetup boardUserSetup;
//
//    @Test
//    public void 내정보_조회() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(formAccount.getLoginId(), formAccount.getPassword(), formAccount.getAuthorities());
//        JWT accessToken = tokenService.createAccessToken(token);
//        em.flush(); em.clear();
//
//        ResultActions perform = mvc.perform(MockMvcRequestBuilders.get("/mypage")
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .accept(MediaType.APPLICATION_JSON)
//                .header(HttpHeaders.AUTHORIZATION, accessToken.getToken())
//        );
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isOk())
//                .andExpect(MockMvcResultMatchers.jsonPath("$.nickname").value(member.getName()));
//    }
//
//    @Test
//    public void 내정보_조회_비로그인() throws Exception {
//        ResultActions perform = mvc.perform(MockMvcRequestBuilders.get("/mypage")
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .accept(MediaType.APPLICATION_JSON)
//        );
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
//    }
//    @Test
//    public void 내정보_변경() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(formAccount.getLoginId(), formAccount.getPassword(), formAccount.getAuthorities());
//        JWT accessToken = tokenService.createAccessToken(token);
//        MyPageDTO.MyInfoChangeRequest request = new MyPageDTO.MyInfoChangeRequest();
//        String nickname = UUID.randomUUID().toString();
//        request.setNickname(nickname);
//
//        em.flush(); em.clear();
//        ResultActions perform = mvc.perform(MockMvcRequestBuilders.put("/mypage/info")
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .accept(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request))
//                .header(HttpHeaders.AUTHORIZATION, accessToken.getToken())
//        );
//
//        em.flush(); em.clear();
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isOk())
//                .andExpect(MockMvcResultMatchers.jsonPath("$.nickname").value(nickname));
//
//        Member findMember = memberService.findByAccountId(formAccount.getAccountId());
//        Assertions.assertEquals(findMember.getName(),nickname);
//    }
//
//    @Test
//    public void 내정보_변경_비로그인() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//        MyPageDTO.MyInfoChangeRequest request = new MyPageDTO.MyInfoChangeRequest();
//        String nickname = UUID.randomUUID().toString();
//        request.setNickname(nickname);
//
//        ResultActions perform = mvc.perform(MockMvcRequestBuilders.put("/mypage/info")
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .accept(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request))
//        );
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
//
//        Account account = accountService.findById(formAccount.getAccountId());
//        Assertions.assertNotEquals(member.getName(),nickname);
//    }
//
//    @Test
//    public void 비밀번호_변경() throws Exception {
//        String nowPassword = UUID.randomUUID().toString();
//        String newPassword = UUID.randomUUID().toString();
//        String loginId = UUID.randomUUID().toString();
//        List<Role> roles = new ArrayList<>();
//        Long formAccountId = accountService.createFormAccount(loginId, "name", nowPassword, roles);
//
//        UsernamePasswordAuthenticationToken token
//                = new UsernamePasswordAuthenticationToken(loginId,null,roles);
//
//        JWT accessToken = tokenService.createAccessToken(token);
//
//        PasswordChangeRequest request = new PasswordChangeRequest();
//        request.setNowPassword(nowPassword);
//        request.setNewPassword(newPassword);
//
//        ResultActions perform = mvc.perform(MockMvcRequestBuilders.post("/mypage/password")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .content(objectMapper.writeValueAsString(request))
//                .header(HttpHeaders.AUTHORIZATION, accessToken.getToken()));
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isOk());
//
//        Account account = accountService.findById(formAccountId);
//
//        Assertions.assertFalse(accountService.matchPassword(account,nowPassword));
//        Assertions.assertTrue(accountService.matchPassword(account,newPassword));
//    }
//
//    @Test
//    public void 비밀번호_변경_기존_비밀번호와_일치하지_않음() throws Exception {
//        String nowPassword = UUID.randomUUID().toString();
//        String newPassword = UUID.randomUUID().toString();
//        String loginId = UUID.randomUUID().toString();
//        List<Role> roles = new ArrayList<>();
//        Long formAccountId = accountService.createFormAccount(loginId, "name", nowPassword, roles);
//
//        UsernamePasswordAuthenticationToken token
//                = new UsernamePasswordAuthenticationToken(loginId,null,roles);
//
//        JWT accessToken = tokenService.createAccessToken(token);
//
//        PasswordChangeRequest request = new PasswordChangeRequest();
//        request.setNowPassword(nowPassword + "1234");
//        request.setNewPassword(newPassword);
//
//        ResultActions perform = mvc.perform(MockMvcRequestBuilders.post("/mypage/password")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .content(objectMapper.writeValueAsString(request))
//                .header(HttpHeaders.AUTHORIZATION, accessToken.getToken()));
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isBadRequest());
//    }
//
//    @Test
//    public void 비밀번호_변경_로그인_상태가_아님() throws Exception {
//        String nowPassword = UUID.randomUUID().toString();
//        String newPassword = UUID.randomUUID().toString();
//
//        PasswordChangeRequest request = new PasswordChangeRequest();
//        request.setNowPassword(nowPassword);
//        request.setNewPassword(newPassword);
//
//        ResultActions perform = mvc.perform(MockMvcRequestBuilders.post("/mypage/password")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .content(objectMapper.writeValueAsString(request)));
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
//    }
//
//
//
//
//
}