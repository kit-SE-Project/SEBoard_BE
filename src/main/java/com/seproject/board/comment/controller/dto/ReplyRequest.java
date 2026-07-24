package com.seproject.board.comment.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

import static com.seproject.board.comment.application.dto.ReplyCommand.*;

public class ReplyRequest {

    @Data
    public static class CreateReplyRequest {
        @NotNull
        private Long postId;
        @NotNull
        private Long superCommentId;
        @NotNull
        private Long tagCommentId;
        @NotNull
        @JsonProperty("isAnonymous")
        private boolean isAnonymous;
        @NotNull
        private String contents;
        @NotNull
        @JsonProperty("isReadOnlyAuthor")
        private boolean isReadOnlyAuthor;
        private List<Long> attachmentIds = Collections.emptyList();

        public ReplyWriteCommand toCommand() {
            return ReplyWriteCommand.builder()
                    .postId(postId)
                    .superCommentId(superCommentId)
                    .tagCommentId(tagCommentId)
                    .contents(contents)
                    .isAnonymous(isAnonymous)
                    .isOnlyReadByAuthor(isReadOnlyAuthor)
                    .attachmentIds(attachmentIds == null ? Collections.emptyList() : attachmentIds)
                    .build();
        }
    }

    @Data
    public static class UpdateReplyRequest {
        @NotNull
        private String contents;
        @NotNull
        @JsonProperty("isReadOnlyAuthor")
        private boolean isReadOnlyAuthor;
        private List<Long> attachmentIds = Collections.emptyList();

        public ReplyEditCommand toCommand(Long replyId) {
            return ReplyEditCommand.builder()
                    .replyId(replyId)
                    .contents(contents)
                    .isOnlyReadByAuthor(isReadOnlyAuthor)
                    .attachmentIds(attachmentIds == null ? Collections.emptyList() : attachmentIds)
                    .build();
        }
    }
}
