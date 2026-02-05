package com.seproject.board.bulletin.controller;

import com.seproject.board.common.BaseTime;
import com.seproject.file.domain.model.FileMetaData;
import com.seproject.global.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BannerControllerTest extends IntegrationTestSupport {
//
//
//    static final String url = "/banners/";
//
//
//    @Test
//    public void 배너_조회() throws Exception {
//        for (int i = 0; i < 3; i++) {
//            FileMetaData img = fileMetaDataSetup.createImageFileMetaData(new BaseTime(), "jpg");
//            bannerSetup.createActiveBanner(img);
//        }
//
//        em.flush(); em.clear();
//
//        ResultActions perform = mvc.perform(MockMvcRequestBuilders.get(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//        );
//
//        perform
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.size()").value(3));
//
//        for (int i = 0; i < 3; i++) {
//            perform
//                    .andExpect(jsonPath("$[" + i + "].fileMetaData").exists())
//                    .andExpect(jsonPath("$[" + i + "].bannerUrl").exists());
//        }
//    }
//
//
}