package com.seproject.board.common.controller.dto;

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
