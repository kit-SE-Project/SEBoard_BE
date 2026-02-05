package com.seproject.account.account.controller;

import com.seproject.account.account.controller.dto.RegisterDTO.OAuth2RegisterRequest;
import com.seproject.account.account.domain.Account;
import com.seproject.account.account.domain.FormAccount;
import com.seproject.account.account.domain.OAuthAccount;
import com.seproject.account.email.domain.AccountRegisterConfirmedEmail;
import com.seproject.account.role.domain.Role;
import com.seproject.global.IntegrationTestSupport;
import com.seproject.member.domain.Member;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.UUID;

import static com.seproject.account.account.controller.dto.RegisterDTO.ConfirmDuplicateNicknameRequest;
import static com.seproject.account.account.controller.dto.RegisterDTO.FormRegisterRequest;

class RegisterControllerTest extends IntegrationTestSupport {
//
//
//    @Test
//    public void OAuth_회원가입() throws Exception {
//        String email = UUID.randomUUID().toString() + "@gmail.com";
//        OAuth2RegisterRequest request = new OAuth2RegisterRequest();
//        request.setSubject("12345678");
//        request.setProvider("kakao");
//        request.setEmail(email);
//        request.setPassword(UUID.randomUUID().toString());
//        request.setName("name");
//        request.setNickname("nickname");
//
//        accountRegisterConfirmedEmailRepository.save(new AccountRegisterConfirmedEmail(email));
//
//        ResultActions perform = mvc.perform(MockMvcRequestBuilders.post("/account/oauth")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .content(objectMapper.writeValueAsString(request))
//        );
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isOk())
//                .andExpect(MockMvcResultMatchers.jsonPath("$.accessToken").isNotEmpty())
//                .andExpect(MockMvcResultMatchers.jsonPath("$.refreshToken").isNotEmpty());
//
//        OAuthAccount oAuthAccount = oAuthAccountRepository.findByLoginId(email).orElseThrow();
//        Role roleUser = roleSetup.getRoleUser();
//        Assertions.assertEquals(oAuthAccount.getAuthorities().size(),1);
//        Assertions.assertTrue(oAuthAccount.getAuthorities().contains(roleUser));
//        Member member = memberService.findByLoginId(email);
//        Assertions.assertEquals(member.getAccount(),oAuthAccount);
//    }
//
//    @Test
//    public void OAuth_회원가입_미인증_이메일() throws Exception {
//        String email = UUID.randomUUID().toString() + "@gmail.com";
//        OAuth2RegisterRequest request = new OAuth2RegisterRequest();
//        request.setSubject("12345678");
//        request.setProvider("kakao");
//        request.setEmail(email);
//        request.setPassword(UUID.randomUUID().toString());
//        request.setName("name");
//        request.setNickname("nickname");
//
//        ResultActions perform = mvc.perform(MockMvcRequestBuilders.post("/account/oauth")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .content(objectMapper.writeValueAsString(request))
//        );
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isNotFound());
//    }
//
//    @Test
//    public void OAuth_회원가입_이미_존재하는_닉네임() throws Exception {
//
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        String email = UUID.randomUUID().toString() + "@gmail.com";
//        OAuth2RegisterRequest request = new OAuth2RegisterRequest();
//        request.setSubject("12345678");
//        request.setProvider("kakao");
//        request.setEmail(email);
//        request.setPassword(UUID.randomUUID().toString());
//        request.setName("name");
//        request.setNickname(member.getName());
//
//        accountRegisterConfirmedEmailRepository.save(new AccountRegisterConfirmedEmail(email));
//
//        em.flush(); em.clear();
//
//        ResultActions perform = mvc.perform(MockMvcRequestBuilders.post("/account/oauth")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .content(objectMapper.writeValueAsString(request))
//        );
//
//        em.flush(); em.clear();
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isBadRequest());
//    }
//
//
//    @Test
//    public void form회원가입() throws Exception {
//        String email = UUID.randomUUID().toString() + "@kumoh.ac.kr";
//        String password = UUID.randomUUID().toString();
//        String nickname = "nickname";
//        String name = "name";
//
//        FormRegisterRequest request = new FormRegisterRequest();
//        request.setId(email);
//        request.setPassword(password);
//        request.setNickname(nickname);
//        request.setName(name);
//
//        accountRegisterConfirmedEmailRepository.save(new AccountRegisterConfirmedEmail(email));
//
//        em.flush(); em.clear();
//
//        ResultActions perform = mvc.perform(MockMvcRequestBuilders.post("/account/form")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .content(objectMapper.writeValueAsString(request)));
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isOk())
//                .andExpect(MockMvcResultMatchers.jsonPath("$.accessToken").isNotEmpty())
//                .andExpect(MockMvcResultMatchers.jsonPath("$.refreshToken").isNotEmpty());
//
//        em.flush(); em.clear();
//
//        Account account = accountRepository.findByLoginIdWithRole(email).orElseThrow();
//        Member fineMember = memberService.findByAccountId(account.getAccountId());
//        Assertions.assertEquals(fineMember.getName(),nickname);
//        Assertions.assertEquals(account.getName(),name);
//        Assertions.assertTrue(accountService.matchPassword(account,password));
//    }
//
//    @Test
//    public void form회원가입_미인증_이메일() throws Exception {
//        String email = UUID.randomUUID().toString() + "@kumoh.ac.kr";
//        String password = UUID.randomUUID().toString();
//        String nickname = "nickname";
//        String name = "name";
//
//        FormRegisterRequest request = new FormRegisterRequest();
//        request.setId(email);
//        request.setPassword(password);
//        request.setNickname(nickname);
//        request.setName(name);
//
//        ResultActions perform = mvc.perform(MockMvcRequestBuilders.post("/account/form")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .content(objectMapper.writeValueAsString(request)));
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isNotFound());
//    }
//
//    @Test
//    public void form회원가입_이미_존재하는_닉네임() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//        String email = UUID.randomUUID().toString() + "@kumoh.ac.kr";
//        String password = UUID.randomUUID().toString();
//        String nickname = member.getName();
//        String name = "name";
//
//        FormRegisterRequest request = new FormRegisterRequest();
//        request.setId(email);
//        request.setPassword(password);
//        request.setNickname(nickname);
//        request.setName(name);
//
//        accountRegisterConfirmedEmailRepository.save(new AccountRegisterConfirmedEmail(email));
//
//        em.flush(); em.clear();
//        ResultActions perform = mvc.perform(MockMvcRequestBuilders.post("/account/form")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .content(objectMapper.writeValueAsString(request)));
//
//        em.flush(); em.clear();
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isBadRequest());
//    }
//
//    @Test
//    public void 닉네임_중복_체크_미중복() throws Exception {
//        String nickname = UUID.randomUUID().toString();
//        ConfirmDuplicateNicknameRequest request = new ConfirmDuplicateNicknameRequest();
//        request.setNickname(nickname);
//
//        ResultActions perform = mvc.perform(MockMvcRequestBuilders.post("/account/nickname")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .content(objectMapper.writeValueAsString(request)));
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isOk())
//                .andExpect(MockMvcResultMatchers.jsonPath("$.duplication").value(false));
//    }
//
//    @Test
//    public void 닉네임_중복_체크_중복() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//        String nickname = member.getName();
//        ConfirmDuplicateNicknameRequest request = new ConfirmDuplicateNicknameRequest();
//        request.setNickname(nickname);
//
//        ResultActions perform = mvc.perform(MockMvcRequestBuilders.post("/account/nickname")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .content(objectMapper.writeValueAsString(request)));
//
//        perform.andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isOk())
//                .andExpect(MockMvcResultMatchers.jsonPath("$.duplication").value(true));
//    }
//
//
//
//
//
//
//
//
}