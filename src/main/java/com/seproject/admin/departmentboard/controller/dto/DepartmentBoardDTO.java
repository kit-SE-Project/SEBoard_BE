package com.seproject.admin.departmentboard.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

public class DepartmentBoardDTO {

    public enum DepartmentBoardSource {
        NOTICE("notice"),
        FREE("freeboard");

        private final String boardUrlInfo;

        DepartmentBoardSource(String boardUrlInfo) {
            this.boardUrlInfo = boardUrlInfo;
        }

        public String getBoardUrlInfo() {
            return boardUrlInfo;
        }
    }

    @Data
    public static class DepartmentBoardRequest {
        private DepartmentBoardSource source;
        private String fromDate;
        private String toDate;
        private boolean includeAttachments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentBoardResult {
        private String jobId;
        private String status;
        private String downloadFileName;
        private int totalCount;
        private int successCount;
        private int failCount;

        @Builder.Default
        private List<DepartmentBoardSuccess> successes = new ArrayList<>();

        @Builder.Default
        private List<DepartmentBoardFailure> failures = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentBoardSuccess {
        private Long articleNo;
        private String title;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentBoardFailure {
        private Long articleNo;
        private String reason;
        private String message;
    }
}
