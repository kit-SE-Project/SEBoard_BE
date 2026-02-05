package com.seproject.board.post.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seproject.board.post.application.dto.PostCommand.PostWriteCommand;
import com.seproject.board.post.domain.model.exposeOptions.ExposeState;
import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

import static com.seproject.board.post.application.dto.PostCommand.*;

public class PostRequest {

    @Data
    @Builder(access = AccessLevel.PRIVATE)
    public static class RetrievePostListRequest {
        private Long categoryId;
        private Integer page;
        private Integer perPage;

    }

    @Data
    public static class RetrievePrivacyPostRequest {
        private String password = " ";
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatePostRequest {
        @NotNull
        private String title;
        @NotNull
        private String contents;
        private List<Long> attachmentIds = new ArrayList<>();
        @NotNull
        private Long categoryId;
        @NotNull
        private boolean pined;
        @NotNull
        private @Valid ExposeOptionRequest exposeOption;
        @NotNull
        @JsonProperty("anonymous")
        private boolean isAnonymous;
        @JsonProperty("isSyncOldVersion")
        private boolean isSyncOldVersion = false;

        public PostWriteCommand toCommand() {
            return PostWriteCommand.builder()
                    .title(title)
                    .contents(getContents())
                    .categoryId(categoryId)
                    .pined(pined)
                    .exposeState(ExposeState.valueOf(exposeOption.getName()))
                    .privatePassword(exposeOption.getPassword())
                    .anonymous(isAnonymous)
                    .attachmentIds(attachmentIds)
                    .isSyncOldVersion(isSyncOldVersion)
                    .build();
        }
    }

    @Data
    public static class UpdatePostRequest {
        @NotNull
        private String title;
        @NotNull
        private String contents;
        private List<Long> attachmentIds = new ArrayList<>();
        @NotNull
        private Long categoryId;
        @NotNull
        private boolean pined;
        @NotNull
        private ExposeOptionRequest exposeOption;

        public PostEditCommand toCommand(Long postId) {
            return PostEditCommand.builder()
                    .postId(postId)
                    .title(title)
                    .contents(getContents())
                    .categoryId(categoryId)
                    .pined(pined)
                    .exposeState(ExposeState.valueOf(exposeOption.getName()))
                    .privatePassword(exposeOption.getPassword())
                    .attachmentIds(attachmentIds)
                    .build();
        }
    }

}
