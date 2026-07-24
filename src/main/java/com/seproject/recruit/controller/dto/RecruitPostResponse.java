package com.seproject.recruit.controller.dto;

import com.seproject.file.domain.model.FileMetaData;
import com.seproject.member.controller.dto.UserResponse;
import com.seproject.recruit.domain.model.RecruitPost;
import com.seproject.skill.domain.SkillTag;
import lombok.Data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class RecruitPostResponse {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Data
    public static class RecruitPostListItem {
        private final Long id;
        private final String type;
        private final String tag;
        private final String title;
        private final String status;
        private final LocalDate endDate;
        private final String createdAt;
        private final String modifiedAt;
        private final List<SkillInfo> skills;
        private final long viewCount;
        private final String portfolioUrl;
        private final UserResponse author;

        public RecruitPostListItem(RecruitPost p) {
            this.id = p.getId();
            this.type = p.getType().name();
            this.tag = p.getTag().name();
            this.title = p.getTitle();
            this.status = p.getStatus().name();
            this.endDate = p.getEndDate();
            this.createdAt = p.getCreatedAt() != null ? p.getCreatedAt().format(DT_FMT) : null;
            this.modifiedAt = p.getModifiedAt() != null ? p.getModifiedAt().format(DT_FMT) : null;
            this.skills = p.getSkillTags().stream().map(SkillInfo::new).collect(Collectors.toList());
            this.viewCount = p.getViewCount();
            this.portfolioUrl = p.getPortfolioUrl();
            this.author = new UserResponse(p.getAuthor());
        }
    }

    @Data
    public static class RecruitPostDetail {
        private final Long id;
        private final String type;
        private final String tag;
        private final String title;
        private final String status;
        private final LocalDate endDate;
        private final String createdAt;
        private final String modifiedAt;
        private final List<SkillInfo> skills;
        private final long viewCount;
        private final UserResponse author;
        private final String contents;
        private final Integer headcount;
        private final LocalDate startDate;
        private final String portfolioUrl;
        private final List<AttachmentInfo> attachments;

        public RecruitPostDetail(RecruitPost p, List<FileMetaData> files) {
            this.id = p.getId();
            this.type = p.getType().name();
            this.tag = p.getTag().name();
            this.title = p.getTitle();
            this.status = p.getStatus().name();
            this.endDate = p.getEndDate();
            this.createdAt = p.getCreatedAt() != null ? p.getCreatedAt().format(DT_FMT) : null;
            this.modifiedAt = p.getModifiedAt() != null ? p.getModifiedAt().format(DT_FMT) : null;
            this.skills = p.getSkillTags().stream().map(SkillInfo::new).collect(Collectors.toList());
            this.viewCount = p.getViewCount();
            this.author = new UserResponse(p.getAuthor());
            this.contents = p.getContents();
            this.headcount = p.getHeadcount();
            this.startDate = p.getStartDate();
            this.portfolioUrl = p.getPortfolioUrl();
            this.attachments = files.stream()
                    .map(f -> new AttachmentInfo(f.getFileMetaDataId(), f.getOriginalFileName(), f.getUrlPath()))
                    .collect(Collectors.toList());
        }
    }

    @Data
    public static class SkillInfo {
        private final Long id;
        private final String name;
        private final String iconSlug;

        public SkillInfo(SkillTag tag) {
            this.id = tag.getId();
            this.name = tag.getName();
            this.iconSlug = tag.getIconSlug();
        }
    }

    @Data
    public static class AttachmentInfo {
        private final Long fileMetaDataId;
        private final String originalFileName;
        private final String url;
    }
}
