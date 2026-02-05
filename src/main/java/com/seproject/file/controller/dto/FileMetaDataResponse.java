package com.seproject.file.controller.dto;

import com.seproject.file.domain.model.FileMetaData;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FileMetaDataResponse {
    @Data
    public static class FileMetaDataListResponse {
        private List<FileMetaDataElement> fileMetaDataList = new ArrayList<>();

        public FileMetaDataListResponse(List<FileMetaData> fileMetaData) {
            fileMetaDataList = fileMetaData.stream().map(data -> new FileMetaDataElement(
                    data.getFileMetaDataId(),
                    data.getOriginalFileName(),
                    data.getStoredFileName(),
                    data.getUrlPath()
            )).collect(Collectors.toList());
        }
    }

    @Data
    public static class FileMetaDataElement {
        private Long fileMetaDataId;
        private String originalFileName;
        private String storedFileName;
        private String url;

        public FileMetaDataElement(FileMetaData fileMetaData) {
            this.fileMetaDataId = fileMetaData.getFileMetaDataId();
            this.originalFileName = fileMetaData.getOriginalFileName();
            this.storedFileName = fileMetaData.getStoredFileName();
            this.url = fileMetaData.getUrlPath();
        }

        public FileMetaDataElement(Long fileMetaDataId, String originalFileName, String storedFileName, String url) {
            this.fileMetaDataId = fileMetaDataId;
            this.originalFileName = originalFileName;
            this.storedFileName = storedFileName;
            this.url = url;
        }
    }
}
