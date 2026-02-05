package com.seproject.file.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

public class FileRequest {
    @Data
    public static class FileConfigurationRequest{
        private Long maxSizePerFile;
        private Long maxSizePerPost;
    }
    @Data
    public static class AdminFileRetrieveCondition{
        @JsonProperty("isOrphan")
        private Boolean isOrphan;
        private String SearchOption;
        private String query;
    }
    @Data
    public static class FileExtensionRequest{
        private List<String> extensions;
    }
    @Data
    public static class BulkFileRequest{
        private List<Long> fileIds;
    }
}
