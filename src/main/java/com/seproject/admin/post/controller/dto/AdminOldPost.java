package com.seproject.admin.post.controller.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminOldPost {
    private String author;
    private String title;
    private String contents;
    private int views;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long categoryId;
    private List<OldCommentRequest> comments;

    @Data
    public static class OldCommentRequest{
        private String author;
        private String contents;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<OldReplyRequest> replies;
    }

    @Data
    public static class OldReplyRequest{
        private String author;
        private String contents;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
