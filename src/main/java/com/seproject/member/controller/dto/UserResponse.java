package com.seproject.member.controller.dto;

import com.seproject.account.role.domain.Role;
import com.seproject.member.domain.BoardUser;
import com.seproject.member.domain.Member;
import lombok.Data;

import java.util.Comparator;
import java.util.List;

@Data
public class UserResponse {

    private Long userId;
    private String name;
    private String profileImageUrl;
    private String frameGradientStart;
    private String frameGradientEnd;
    /** "CHECK" | "KUMOH_CROW" | null */
    private String badgeType;
    /** 툴팁에 표시할 역할 alias (예: "관리자", "금오인") */
    private String badgeLabel;

    public UserResponse(Long userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public UserResponse(BoardUser boardUser) {
        if(!boardUser.isAnonymous()){
            this.userId = boardUser.getBoardUserId();
            if (boardUser instanceof Member) {
                Member member = (Member) boardUser;
                if (member.getEquippedFrame() != null) {
                    this.frameGradientStart = member.getEquippedFrame().getGradientStart();
                    this.frameGradientEnd = member.getEquippedFrame().getGradientEnd();
                }
                applyBadge(boardUser.getAccount().getRoles());
            }
        }
        this.name = boardUser.getName();
    }

    private void applyBadge(List<Role> roles) {
        roles.stream()
                .filter(r -> r.getBadgeType() != null && r.getBadgePriority() != null)
                .min(Comparator.comparingInt(Role::getBadgePriority))
                .ifPresent(r -> {
                    this.badgeType = r.getBadgeType();
                    this.badgeLabel = r.toString(); // alias
                });
    }
}
