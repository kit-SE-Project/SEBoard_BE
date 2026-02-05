package com.seproject.board.comment.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seproject.board.comment.application.dto.CommentCommand.CommentEditCommand;
import com.seproject.board.comment.application.dto.CommentCommand.CommentWriteCommand;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

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

        public CommentWriteCommand toCommand() {
            return CommentWriteCommand.builder()
                    .postId(postId)
                    .contents(contents)
                    .isAnonymous(isAnonymous)
                    .isOnlyReadByAuthor(isReadOnlyAuthor)
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
        public CommentEditCommand toCommand(Long commentId) {
            return CommentEditCommand.builder()
                    .commentId(commentId)
                    .contents(contents)
                    .isOnlyReadByAuthor(isReadOnlyAuthor)
                    .build();
        }
    }


}
