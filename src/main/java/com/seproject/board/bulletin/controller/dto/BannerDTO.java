package com.seproject.board.bulletin.controller.dto;

import com.seproject.board.bulletin.domain.model.Banner;
import com.seproject.file.controller.dto.FileMetaDataResponse;
import lombok.Data;

public class BannerDTO {

    @Data
    public static class BannerResponse {
        private FileMetaDataResponse.FileMetaDataElement fileMetaData;
        private String bannerUrl;

        public BannerResponse(Banner banner){
            this.fileMetaData = new FileMetaDataResponse.FileMetaDataElement(banner.getFileMetaData());
            this.bannerUrl = banner.getBannerUrl();
        }
    }
}
