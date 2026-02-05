package com.seproject.admin.ip.controller;

import com.seproject.account.Ip.domain.Ip;
import com.seproject.admin.ip.controller.dto.IpDTO;
import com.seproject.global.IntegrationTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminIpControllerTest extends IntegrationTestSupport {
//
//    static final String url = "/admin/ip/";
//    @Test
//    public void 금지_아이피_목록_조회() throws Exception {
//
//        for (int i = 0; i < 27; i++) {
//            Ip ip = Ip.builder()
//                    .ipAddress(UUID.randomUUID().toString())
//                    .build();
//
//            ipRepository.save(ip);
//        }
//
//        em.flush();
//        em.clear();
//
//        ResultActions perform = mvc.perform(get(url)
//                .queryParam("page",String.valueOf(2))
//                .queryParam("perPage",String.valueOf(11))
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .header("Authorization",accessToken)
//                .characterEncoding("UTF-8"));
//
//        perform.andDo(print())
//                .andExpect(status().isOk());
//
//        perform.andExpect(jsonPath("$.size()").value(27));
//    }
//
//    @Test
//    public void 금지_아이피_삭제() throws Exception {
//        String ip = UUID.randomUUID().toString();
//
//        ipRepository.save(Ip.builder()
//                .ipAddress(ip)
//                .build());
//
//        em.flush();
//        em.clear();
//
//        IpDTO.DeleteIpRequest request = new IpDTO.DeleteIpRequest();
//        request.setIpAddress(ip);
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
//        Optional<Ip> byIpAddress = ipRepository.findByIpAddress(ip);
//        Assertions.assertFalse(byIpAddress.isPresent());
//    }
//
//    @Test
//    public void 금지_아이피_삭제_존재하지_않음() throws Exception {
//        IpDTO.DeleteIpRequest request = new IpDTO.DeleteIpRequest();
//        request.setIpAddress(UUID.randomUUID().toString());
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
//    @Test
//    public void 금지_아이피_추가() throws Exception {
//        String ip = UUID.randomUUID().toString();
//        IpDTO.CreateIpRequest request = new IpDTO.CreateIpRequest();
//        request.setIpAddress(ip);
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
//        Optional<Ip> byIpAddress = ipRepository.findByIpAddress(ip);
//        Assertions.assertTrue(byIpAddress.isPresent());
//    }
//
//
//
}