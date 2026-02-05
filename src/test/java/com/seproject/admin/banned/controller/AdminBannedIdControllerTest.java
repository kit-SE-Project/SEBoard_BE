package com.seproject.admin.banned.controller;

import com.seproject.admin.banned.controller.dto.BannedIdDTO;
import com.seproject.admin.banned.domain.BannedId;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.global.IntegrationTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminBannedIdControllerTest extends IntegrationTestSupport {
//    static final String url = "/admin/accountPolicy/bannedId/";
//    @Test
//    public void 금지_아이디_조회() throws Exception {
//
//        List<BannedId> list = new ArrayList<>();
//
//        for (int i = 0; i < 50; i++) {
//            BannedId bannedId = BannedId.builder()
//                    .bannedId(UUID.randomUUID().toString().substring(0, 8))
//                    .build();
//            list.add(bannedId);
//        }
//        bannedIdRepository.saveAll(list);
//
//        em.flush();
//        em.clear();
//
//        ResultActions perform = mvc.perform(get(url)
//                .queryParam("page",String.valueOf(1))
//                .queryParam("perPage",String.valueOf(15))
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .header("Authorization",accessToken)
//                .characterEncoding("UTF-8"));
//
//        perform.andDo(print())
//                .andExpect(status().isOk());
//
//        perform.andExpect(jsonPath("$.first").value(false))
//                .andExpect(jsonPath("$.totalPages").value(4))
//                .andExpect(jsonPath("$.totalElements").value(50))
//                .andExpect(jsonPath("$.size").value(15))
//                .andExpect(jsonPath("$.number").value(1));
//    }
//
//    @Test
//    public void 금지_아이디_추가() throws Exception {
//        BannedIdDTO.CreateBannedIdRequest request = new BannedIdDTO.CreateBannedIdRequest();
//        String bannedId = UUID.randomUUID().toString();
//        request.setBannedId(bannedId);
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
//        Optional<BannedId> byBannedId = bannedIdRepository.findByBannedId(bannedId);
//        Assertions.assertTrue(byBannedId.isPresent());
//    }
//
//    @Test
//    public void 금지_아이디_추가_이미_존재하는_아이디() throws Exception {
//        String bannedId = UUID.randomUUID().toString();
//        bannedIdRepository.save(new BannedId(1L,bannedId));
//        BannedIdDTO.CreateBannedIdRequest request = new BannedIdDTO.CreateBannedIdRequest();
//        request.setBannedId(bannedId);
//
//        ResultActions perform = mvc.perform(post(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .content(objectMapper.writeValueAsString(request)));
//
//        perform.andDo(print())
//                .andExpect(status().isBadRequest());
//
//        perform
//                .andExpect(jsonPath("$.message")
//                        .value(ErrorCode.ALREADY_EXIST_BANNED_ID.getMessage()))
//                .andExpect(jsonPath("$.code")
//                        .value(ErrorCode.ALREADY_EXIST_BANNED_ID.getCode()));
//    }
//
//    @Test
//    public void 금지_아이디_삭제() throws Exception {
//
//        String bannedId = UUID.randomUUID().toString();
//
//        bannedIdRepository.save(new BannedId(1L,bannedId));
//
//        em.flush();
//        em.clear();
//
//
//        BannedIdDTO.DeleteBannedIdRequest request = new BannedIdDTO.DeleteBannedIdRequest();
//        request.setBannedId(bannedId);
//
//        ResultActions perform = mvc.perform(delete(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .content(objectMapper.writeValueAsString(request)));
//
//        perform.andDo(print())
//                .andExpect(status().isOk());
//
//
//    }
//
//    @Test
//    public void 존재하지_않는_금지_아이디_삭제() throws Exception {
//
//        BannedIdDTO.DeleteBannedIdRequest request = new BannedIdDTO.DeleteBannedIdRequest();
//        request.setBannedId(UUID.randomUUID().toString());
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
}

