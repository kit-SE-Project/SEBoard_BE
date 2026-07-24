package com.seproject.board.common.controller.dto;

import com.seproject.developer.domain.DeveloperProfile;
import com.seproject.member.domain.model.Frame;
import com.seproject.member.domain.model.MemberFrame;
import com.seproject.member.domain.model.Tier;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ProfileResponse {
    @Data
    @Builder
    public static class ProfileInfoResponse {
        private String nickname;
        private Integer postCount;
        private Integer commentCount;
        private Integer bookmarkCount;
        private String profileImageUrl;
        private String tier;
        private Long activityScore;
        private FrameInfo equippedFrame;
        /** "CHECK" | "KUMOH_CROW" | null */
        private String badgeType;
        private String badgeLabel;
        private DeveloperProfileInfo developerProfile;
    }

    @Data
    public static class DeveloperProfileInfo {
        private final Long id;
        private final String intro;
        private final String githubUrl;
        private final String portfolioUrl;
        private final String grade;
        private final String readmeContent;
        private final List<SkillInfo> skills;

        public DeveloperProfileInfo(DeveloperProfile p) {
            this.id = p.getId();
            this.intro = p.getIntro();
            this.githubUrl = p.getGithubUrl();
            this.portfolioUrl = p.getPortfolioUrl();
            this.grade = p.getGrade();
            this.readmeContent = p.getReadmeContent();
            this.skills = p.getSkillTags().stream()
                    .map(s -> new SkillInfo(s.getId(), s.getName(), s.getCategory().name(), s.getIconSlug()))
                    .collect(Collectors.toList());
        }

        @Data
        public static class SkillInfo {
            private final Long id;
            private final String name;
            private final String category;
            private final String iconSlug;
        }
    }

    @Data
    public static class FrameInfo {
        private Long frameId;
        private String name;
        private String description;
        private String gradientStart;
        private String gradientEnd;
        private String frameType;

        public FrameInfo(Frame frame) {
            this.frameId = frame.getFrameId();
            this.name = frame.getName();
            this.description = frame.getDescription();
            this.gradientStart = frame.getGradientStart();
            this.gradientEnd = frame.getGradientEnd();
            this.frameType = frame.getFrameType().name();
        }
    }

    @Data
    public static class MemberFrameInfo {
        private Long memberFrameId;
        private FrameInfo frame;
        private LocalDateTime acquiredAt;

        public MemberFrameInfo(MemberFrame memberFrame) {
            this.memberFrameId = memberFrame.getMemberFrameId();
            this.frame = new FrameInfo(memberFrame.getFrame());
            this.acquiredAt = memberFrame.getAcquiredAt();
        }
    }
}
