package com.seproject.board.comment.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

public class ReplyCommand {
    @Getter
    @Builder
    public static class ReplyWriteCommand{
        private Long postId;
        private Long superCommentId;
        private Long tagCommentId;
        private String contents;
        private boolean isAnonymous;
        private boolean isOnlyReadByAuthor;
        @Builder.Default
        private List<Long> attachmentIds = Collections.emptyList();
    }

    @Getter
    @Builder
    public static class ReplyEditCommand{
        private Long replyId;
        private String contents;
        private boolean isOnlyReadByAuthor;
        @Builder.Default
        private List<Long> attachmentIds = Collections.emptyList();
    }
}
