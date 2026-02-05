package com.seproject.admin.banned.controller;

import com.seproject.admin.banned.domain.BannedNickname;
import com.seproject.global.IntegrationTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.seproject.admin.banned.controller.dto.BannedNicknameDTO.CreateBannedNicknameRequest;
import static com.seproject.admin.banned.controller.dto.BannedNicknameDTO.DeleteBannedNicknameRequest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminBannedNicknameControllerTest extends IntegrationTestSupport {
//
//    static final String url = "/admin/accountPolicy/bannedNickname/";
//
//    @Value("${jwt.test}") String accessToken;
//
//    @Test
//    public void 금지_닉네임_목록_조회() throws Exception {
//
//        List<BannedNickname> list = new ArrayList<>();
//        for (int i = 1; i <= 55; i++) {
//            BannedNickname bannedNickname = BannedNickname.builder()
//                    .bannedNickname(UUID.randomUUID().toString())
//                    .build();
//            list.add(bannedNickname);
//        }
//
//        bannedNicknameRepository.saveAll(list);
//
//        em.flush();
//        em.clear();
//
//        ResultActions perform = mvc.perform(get(url)
//                .queryParam("page", "1")
//                .queryParam("perPage", "22")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .header("Authorization",accessToken)
//                .characterEncoding("UTF-8")
//        );
//
//        perform.andDo(print())
//                .andExpect(status().isOk());
//
//        perform.andExpect(jsonPath("$.first").value(false))
//                .andExpect(jsonPath("$.totalPages").value(3))
//                .andExpect(jsonPath("$.totalElements").value(55))
//                .andExpect(jsonPath("$.size").value(22))
//                .andExpect(jsonPath("$.number").value(1));
//    }
//
//    @Test
//    public void 금지_닉네임_추가() throws Exception {
//
//        String bannedNickname = UUID.randomUUID().toString();
//
//        CreateBannedNicknameRequest request = new CreateBannedNicknameRequest();
//        request.setNickname(bannedNickname);
//
//        ResultActions perform = mvc.perform(post(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .content(objectMapper.writeValueAsString(request)));
//
//        perform.andDo(print())
//                .andExpect(status().isOk());
//
//        em.flush();
//        em.clear();
//
//        Optional<BannedNickname> byBannedNickname = bannedNicknameRepository.findByBannedNickname(bannedNickname);
//
//        Assertions.assertTrue(byBannedNickname.isPresent());
//    }
//
//    @Test
//    public void 금지_닉네임_추가_이미_존재() throws Exception {
//
//        String bannedNickname = UUID.randomUUID().toString();
//
//        bannedNicknameRepository.save(BannedNickname.builder()
//                .bannedNickname(bannedNickname).build());
//
//        em.flush();
//        em.clear();
//
//        CreateBannedNicknameRequest request = new CreateBannedNicknameRequest();
//        request.setNickname(bannedNickname);
//
//        ResultActions perform = mvc.perform(post(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .content(objectMapper.writeValueAsString(request)));
//
//        em.flush();
//        em.clear();
//
//        perform.andDo(print())
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    public void 금지_닉네임_삭제() throws Exception {
//
//        String bannedNickname = UUID.randomUUID().toString();
//        bannedNicknameRepository.save(BannedNickname.builder()
//                .bannedNickname(bannedNickname).build());
//
//        DeleteBannedNicknameRequest request = new DeleteBannedNicknameRequest();
//        request.setNickname(bannedNickname);
//
//        em.flush();
//        em.clear();
//
//        ResultActions perform = mvc.perform(delete(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .content(objectMapper.writeValueAsString(request)));
//
//        em.flush();
//        em.clear();
//
//        perform.andDo(print())
//                .andExpect(status().isOk());
//
//        Optional<BannedNickname> byBannedNickname = bannedNicknameRepository.findByBannedNickname(bannedNickname);
//
//        Assertions.assertTrue(byBannedNickname.isEmpty());
//    }
//
//    @Test
//    public void 금지_닉네임_삭제_존재하지_않음() throws Exception {
//
//        String bannedNickname = UUID.randomUUID().toString();
//
//        DeleteBannedNicknameRequest request = new DeleteBannedNicknameRequest();
//        request.setNickname(bannedNickname);
//
//        ResultActions perform = mvc.perform(delete(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .content(objectMapper.writeValueAsString(request)));
//
//        perform.andDo(print())
//                .andExpect(status().isNotFound());
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

}