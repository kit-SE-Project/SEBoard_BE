package com.seproject.board.comment.application.dto;

import lombok.Builder;
import lombok.Data;

public class CommentCommand {
    @Data
    @Builder
    public static class CommentWriteCommand{
        private Long postId;
        private String contents;
        private boolean isAnonymous;
        private boolean isOnlyReadByAuthor;
    }

    @Data
    @Builder
    public static class CommentEditCommand{
        private Long commentId;
        private String contents;
        private boolean isOnlyReadByAuthor;
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
