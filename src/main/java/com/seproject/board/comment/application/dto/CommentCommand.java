package com.seproject.board.comment.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;

public class CommentCommand {
    @Data
    @Builder
    public static class CommentWriteCommand{
        private Long postId;
        private String contents;
        private boolean isAnonymous;
        private boolean isOnlyReadByAuthor;
        @Builder.Default
        private List<Long> attachmentIds = Collections.emptyList();
    }

    @Data
    @Builder
    public static class CommentEditCommand{
        private Long commentId;
        private String contents;
        private boolean isOnlyReadByAuthor;
        @Builder.Default
        private List<Long> attachmentIds = Collections.emptyList();
    }

    @Data
    @Builder
    public static class CommentListFindCommand {
        private Long postId;
        private String password;
        private Integer page;
        private Integer perPage;
    }
}
