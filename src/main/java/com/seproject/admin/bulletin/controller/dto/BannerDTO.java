package com.seproject.admin.bulletin.controller.dto;

import com.seproject.board.bulletin.domain.model.Banner;
import com.seproject.file.controller.dto.FileMetaDataResponse;
import lombok.Data;

import java.time.LocalDate;

public class BannerDTO {

    @Data
    public static class BannerResponse {
        private Long bannerId;
        private LocalDate startDate;
        private LocalDate endDate;
        private FileMetaDataResponse.FileMetaDataElement fileMetaData;
        private String bannerUrl;

        public BannerResponse(Banner banner) {
            this.bannerId = banner.getBannerId();
            this.startDate = banner.getStartDate();
            this.endDate = banner.getEndDate();
            this.fileMetaData = new FileMetaDataResponse.FileMetaDataElement(banner.getFileMetaData());
            this.bannerUrl = banner.getBannerUrl();
        }
    }

    @Data
    public static class CreateBannerRequest {
        private LocalDate startDate;
        private LocalDate endDate;
        private String bannerUrl;
        private Long fileMetaDataId;
    }

    @Data
    public static class UpdateBannerRequest {
        private LocalDate startDate;
        private LocalDate endDate;
        private String bannerUrl;
        private Long fileMetaDataId;
    }



}
