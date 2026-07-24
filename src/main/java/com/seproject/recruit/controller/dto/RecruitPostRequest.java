package com.seproject.recruit.controller.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class RecruitPostRequest {
    @Data
    public static class Create {
        private String type;
        private String tag;
        private String title;
        private String contents;
        private List<Long> skillIds;
        private String exposeState = "PUBLIC";
        private String privatePassword;
        private Integer headcount;
        private LocalDate startDate;
        private LocalDate endDate;
        private String portfolioUrl;
        private List<Long> attachmentIds = new ArrayList<>();
    }

    @Data
    public static class Update {
        private String type;
        private String tag;
        private String title;
        private String contents;
        private List<Long> skillIds;
        private String exposeState = "PUBLIC";
        private String privatePassword;
        private Integer headcount;
        private LocalDate startDate;
        private LocalDate endDate;
        private String portfolioUrl;
        private List<Long> attachmentIds = new ArrayList<>();
    }
}
