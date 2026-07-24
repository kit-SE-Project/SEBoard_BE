package com.seproject.board.comment.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seproject.board.comment.application.dto.CommentCommand.CommentEditCommand;
import com.seproject.board.comment.application.dto.CommentCommand.CommentWriteCommand;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

public class CommentRequest {

    @Data
    public static class CreateCommentRequest {
        @NotNull
        private Long postId;
        @NotBlank
        private String contents;
        @NotNull
        @JsonProperty("isAnonymous")
        private boolean isAnonymous;
        @NotNull
        @JsonProperty("isReadOnlyAuthor")
        private boolean isReadOnlyAuthor;
        private List<Long> attachmentIds = Collections.emptyList();

        public CommentWriteCommand toCommand() {
            return CommentWriteCommand.builder()
                    .postId(postId)
                    .contents(contents)
                    .isAnonymous(isAnonymous)
                    .isOnlyReadByAuthor(isReadOnlyAuthor)
                    .attachmentIds(attachmentIds == null ? Collections.emptyList() : attachmentIds)
                    .build();
        }
    }

    @Data
    public static class UpdateCommentRequest {
        @NotNull
        private String contents;
        @NotNull
        @JsonProperty("isReadOnlyAuthor")
        private boolean isReadOnlyAuthor;
        private List<Long> attachmentIds = Collections.emptyList();

        public CommentEditCommand toCommand(Long commentId) {
            return CommentEditCommand.builder()
                    .commentId(commentId)
                    .contents(contents)
                    .isOnlyReadByAuthor(isReadOnlyAuthor)
                    .attachmentIds(attachmentIds == null ? Collections.emptyList() : attachmentIds)
                    .build();
        }
    }


}
