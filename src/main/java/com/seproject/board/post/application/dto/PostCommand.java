package com.seproject.board.post.application.dto;

import com.seproject.board.post.domain.model.exposeOptions.ExposeState;
import lombok.Builder;
import lombok.Data;

import java.util.List;

public class PostCommand {
    @Data
    @Builder
    public static class PostWriteCommand {
        private String title;
        private String contents;
        private Long categoryId;
        private boolean pined;
        private ExposeState exposeState;
        private String privatePassword;
        private boolean anonymous;
        private boolean isSyncOldVersion;
        private List<Long> attachmentIds;
    }

    @Data
    @Builder
    public static class PostEditCommand{
        private Long postId;
        private String title;
        private String contents;
        private Long categoryId;
        private boolean pined;
        private ExposeState exposeState;
        private String privatePassword;
        private List<Long> attachmentIds;
    }
}
