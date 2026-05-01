package com.seproject.notification;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationEventDto {

    private String type;
    private Long receiverId;     // null이면 boardMenuId 기반 브로드캐스트
    private String actorName;
    private Long relatedId;      // postId
    private String title;
    private String content;
    private Long boardMenuId;    // NEW_POST 팬아웃용
}
