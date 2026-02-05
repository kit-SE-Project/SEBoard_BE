package com.seproject.admin.bulletin.controller;

import com.seproject.board.bulletin.domain.model.Banner;
import com.seproject.board.common.BaseTime;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.NoSuchResourceException;
import com.seproject.file.domain.model.FileMetaData;
import com.seproject.global.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.seproject.admin.bulletin.controller.dto.BannerDTO.CreateBannerRequest;
import static com.seproject.admin.bulletin.controller.dto.BannerDTO.UpdateBannerRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminBannerControllerTest extends IntegrationTestSupport {
//    static final String url = "/admin/banners/";
//
//    //TODO : 배너 조회 시간 너무 오래걸림
//    @Test
//    public void 활성화_배너_조회() throws Exception {
//
//        for (int i = 0; i < 3; i++) {
//            FileMetaData fileMetaData = fileMetaDataSetup.createImageFileMetaData(new BaseTime(),"jpg");
//            bannerSetup.createInActiveBanner(fileMetaData,true);
//        }
//
//        for (int i = 0; i < 18; i++) {
//            FileMetaData fileMetaData = fileMetaDataSetup.createImageFileMetaData(new BaseTime(),"png");
//            bannerSetup.createActiveBanner(fileMetaData);
//        }
//
//        em.flush(); em.clear();
//
//        mvc.perform(get(url)
//                .accept(MediaType.APPLICATION_JSON)
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .param("isActive","true")
//                .param("page","2")
//                .param("perPage","5")
//        ).andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.size()").value(18));
//    }
//    @Test
//    public void 비활성화_배너조회() throws Exception {
//        for (int i = 0; i < 3; i++) {
//            FileMetaData fileMetaData = fileMetaDataSetup.createImageFileMetaData(new BaseTime(),"jpg");
//            bannerSetup.createInActiveBanner(fileMetaData,true);
//        }
//
//        for (int i = 0; i < 18; i++) {
//            FileMetaData fileMetaData = fileMetaDataSetup.createImageFileMetaData(new BaseTime(),"png");
//            bannerSetup.createActiveBanner(fileMetaData);
//        }
//
//        mvc.perform(get(url)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .header("Authorization",accessToken)
//                        .param("isActive","false")
//                        .param("page","0")
//                        .param("perPage","5")
//                ).andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.size()").value(3));
//    }
//    @Test
//    public void 배너조회() throws Exception {
//        for (int i = 0; i < 3; i++) {
//            FileMetaData fileMetaData = fileMetaDataSetup.createImageFileMetaData(new BaseTime(),"jpg");
//            bannerSetup.createInActiveBanner(fileMetaData,true);
//        }
//
//        for (int i = 0; i < 18; i++) {
//            FileMetaData fileMetaData = fileMetaDataSetup.createImageFileMetaData(new BaseTime(),"png");
//            bannerSetup.createActiveBanner(fileMetaData);
//        }
//
//        mvc.perform(get(url)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .header("Authorization",accessToken)
//                        .param("page","0")
//                        .param("perPage","5")
//                ).andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.size()").value(21));
//    }
//    @Test
//    public void 배너_생성() throws Exception {
//        FileMetaData jpg = fileMetaDataSetup.createImageFileMetaData(new BaseTime(), "jpg");
//
//        CreateBannerRequest request = new CreateBannerRequest();
//        request.setStartDate(LocalDate.now().plusDays(1));
//        request.setEndDate(LocalDate.now().plusDays(6));
//        request.setBannerUrl(UUID.randomUUID().toString().substring(0,8));
//        request.setFileMetaDataId(jpg.getFileMetaDataId());
//
//
//        em.flush(); em.clear();
//
//        mvc.perform(post(url)
//                .accept(MediaType.APPLICATION_JSON)
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization", accessToken)
//                .content(objectMapper.writeValueAsString(request))
//        ).andDo(print())
//                .andExpect(status().isOk());
//
//        List<Banner> all = bannerService.findAll();
//
//        assertEquals(all.size(),1);
//        Banner banner = all.get(0);
//
//        assertEquals(banner.getStartDate(),request.getStartDate());
//        assertEquals(banner.getEndDate(),request.getEndDate());
//        assertEquals(banner.getBannerUrl(),request.getBannerUrl());
//        assertEquals(banner.getFileMetaData().getFileMetaDataId(),request.getFileMetaDataId());
//    }
//    @Test
//    public void 배너_생성_파일_존재하지_않음() throws Exception {
//        CreateBannerRequest request = new CreateBannerRequest();
//        request.setStartDate(LocalDate.now().plusDays(1));
//        request.setEndDate(LocalDate.now().plusDays(6));
//        request.setBannerUrl(UUID.randomUUID().toString().substring(0,8));
//        request.setFileMetaDataId(5453L);
//
//        mvc.perform(post(url)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .header("Authorization", accessToken)
//                        .content(objectMapper.writeValueAsString(request))
//                ).andDo(print())
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_EXIST_FILE.getCode()));
//    }
//    @Test
//    public void 배너_생성_끝날짜가_시작날짜보다_빠름() throws Exception {
//        FileMetaData jpg = fileMetaDataSetup.createImageFileMetaData(new BaseTime(), "jpg");
//
//        CreateBannerRequest request = new CreateBannerRequest();
//        request.setStartDate(LocalDate.now().plusDays(1));
//        request.setEndDate(LocalDate.now().minusDays(6));
//        request.setBannerUrl(UUID.randomUUID().toString().substring(0,8));
//        request.setFileMetaDataId(jpg.getFileMetaDataId());
//
//        em.flush(); em.clear();
//
//        mvc.perform(post(url)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .header("Authorization", accessToken)
//                        .content(objectMapper.writeValueAsString(request))
//                ).andDo(print())
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_DATE.getCode()));
//
//    }
//    @Test
//    public void 배너_수정() throws Exception {
//        FileMetaData img = fileMetaDataSetup.createImageFileMetaData(new BaseTime(), "jpg");
//        Banner banner = bannerSetup.createActiveBanner(img);
//
//        FileMetaData newImg = fileMetaDataSetup.createImageFileMetaData(new BaseTime(),"png");
//
//        UpdateBannerRequest request = new UpdateBannerRequest();
//        request.setStartDate(LocalDate.now().plusDays(1));
//        request.setEndDate(LocalDate.now().plusDays(6));
//        request.setBannerUrl(UUID.randomUUID().toString().substring(0,8));
//        request.setFileMetaDataId(newImg.getFileMetaDataId());
//
//        em.flush(); em.clear();
//
//        mvc.perform(put(url + "{bannerId}" , banner.getBannerId())
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .content(objectMapper.writeValueAsString(request))
//                .header("Authorization",accessToken)
//        ).andDo(print())
//                .andExpect(status().isOk());
//
//        Banner findBanner = bannerService.findById(banner.getBannerId());
//
//        assertEquals(findBanner.getStartDate(),request.getStartDate());
//        assertEquals(findBanner.getEndDate(),request.getEndDate());
//        assertEquals(findBanner.getBannerUrl(),request.getBannerUrl());
//        assertEquals(findBanner.getFileMetaData().getFileMetaDataId(),request.getFileMetaDataId());
//    }
//    @Test
//    public void 존재하지_않는_배너_수정() throws Exception {
//        UpdateBannerRequest request = new UpdateBannerRequest();
//        request.setStartDate(LocalDate.now().plusDays(1));
//        request.setEndDate(LocalDate.now().plusDays(6));
//        request.setBannerUrl(UUID.randomUUID().toString().substring(0,8));
//        request.setFileMetaDataId(5453L);
//
//        mvc.perform(put(url + "{bannerId}" , 1243L)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .content(objectMapper.writeValueAsString(request))
//                        .header("Authorization",accessToken)
//                ).andDo(print())
//                .andExpect(status().isNotFound());
//    }
//    @Test
//    public void 배너_수정_파일_없음() throws Exception {
//        FileMetaData img = fileMetaDataSetup.createImageFileMetaData(new BaseTime(), "jpg");
//        Banner banner = bannerSetup.createActiveBanner(img);
//        UpdateBannerRequest request = new UpdateBannerRequest();
//        request.setStartDate(LocalDate.now().plusDays(1));
//        request.setEndDate(LocalDate.now().plusDays(6));
//        request.setBannerUrl(UUID.randomUUID().toString().substring(0,8));
//        request.setFileMetaDataId(5453L);
//
//        em.flush(); em.clear();
//
//        mvc.perform(put(url + "{bannerId}" , banner.getBannerId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .content(objectMapper.writeValueAsString(request))
//                        .header("Authorization",accessToken)
//                ).andDo(print())
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    public void 배너_삭제() throws Exception {
//        FileMetaData img = fileMetaDataSetup.createImageFileMetaData(new BaseTime(), "jpg");
//        Banner banner = bannerSetup.createInActiveBanner(img,true);
//
//        em.flush(); em.clear();
//
//        mvc.perform(delete(url + "{bannerId}" , banner.getBannerId())
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//        ).andDo(print())
//                .andExpect(status().isOk());
//
//        NoSuchResourceException ex = assertThrows(NoSuchResourceException.class, () -> {
//            bannerService.findById(banner.getBannerId());
//        });
//
//        assertEquals(ex.getErrorCode(),ErrorCode.NOT_EXIST_BANNER);
//    }
//
//    @Test
//    public void 존재하지_않는_배너_삭제() throws Exception {
//        mvc.perform(delete(url + "{bannerId}" , 1431423L)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .header("Authorization",accessToken)
//                ).andDo(print())
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
}