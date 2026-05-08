package com.seproject.notification;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationEventDto {

    private String type;
    private Long receiverId;      // null이면 boardMenuId 기반 또는 globalBroadcast
    private String actorName;
    private Long relatedId;       // postId
    private String title;
    private String content;
    private Long boardMenuId;     // NEW_POST 팬아웃용
    private Long authorId;        // 자기 자신 알림 제외용
    private boolean globalBroadcast; // true이면 전체 사용자 대상 브로드캐스트
}
