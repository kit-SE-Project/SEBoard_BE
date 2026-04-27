package com.seproject.board.post.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seproject.file.controller.dto.FileMetaDataResponse.FileMetaDataListResponse;
import com.seproject.board.comment.controller.dto.PaginationResponse;
import com.seproject.member.controller.dto.UserResponse;
import com.seproject.board.menu.domain.model.Category;
import com.seproject.board.post.domain.model.Post;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PostResponse {

    @Data
    public static class RetrievePostListResponseElement {
        private Long postId;
        private String title;
        private PostDetailCategoryResponse category;
        private UserResponse author;
        private Integer views;
        private LocalDateTime createdAt;
        private LocalDateTime modifiedAt;
        private boolean hasAttachment;
        private Integer commentSize;
        private boolean pined;

        public RetrievePostListResponseElement(Post post){
            this.postId = post.getPostId();
            this.title = post.getTitle();
            this.category = new PostDetailCategoryResponse(post.getCategory());
            this.author = new UserResponse(post.getAuthor());
            this.views = post.getViews();
            this.createdAt = post.getBaseTime().getCreatedAt();
            this.modifiedAt = post.getBaseTime().getModifiedAt();
            this.hasAttachment = post.hasAttachments();
            this.pined = post.isPined();
        }
    }

    @Data
    @Builder(access = AccessLevel.PRIVATE)
    public static class RetrievePinedPostListResponse {
        private RetrievePostListResponse pined;
        private RetrievePostListResponse normal;

        public static RetrievePinedPostListResponse toDTO(RetrievePostListResponse pined, RetrievePostListResponse normal) {
            return builder()
                    .pined(pined)
                    .normal(normal)
                    .build();
        }
    }

    @Data
    @Builder(access = AccessLevel.PRIVATE)
    public static class RetrievePostListResponse {
        private PaginationResponse paginationInfo;
        private List<RetrievePostListResponseElement> posts;

        public static RetrievePostListResponse toDTO(List<RetrievePostListResponseElement> posts, PaginationResponse paginationInfo) {

            return builder()
                    .paginationInfo(paginationInfo)
                    .posts(posts)
                    .build();
        }
    }

    @Data
    public static class RetrievePostDetailResponse {
        @Schema(description = "게시물 pk")
        private Long postId;
        private String title;
        private String contents;
        private PostDetailCategoryResponse category;
        private UserResponse author;
        private Integer views;
        private LocalDateTime createdAt;
        private LocalDateTime modifiedAt;
        @JsonProperty("isEditable")
        private boolean isEditable;
        @JsonProperty("isBookmarked")
        private boolean isBookmarked;
        @JsonProperty("isPined")
        private boolean isPined;
        private String exposeType;
        private FileMetaDataListResponse attachments;
        private int likeCount;
        private int dislikeCount;
        private String myReaction;


        public RetrievePostDetailResponse(Post post) {
            this.postId = post.getPostId();
            this.title = post.getTitle();
            this.contents = post.getContents();
            this.category = new PostDetailCategoryResponse(post.getCategory());
            this.author = new UserResponse(post.getAuthor());
            this.views = post.getViews();
            this.createdAt = post.getBaseTime().getCreatedAt();
            this.modifiedAt = post.getBaseTime().getModifiedAt();
            this.exposeType = post.getExposeOption().getExposeState().toString();
            this.attachments = new FileMetaDataListResponse(new ArrayList<>(post.getAttachments()));
            this.isPined = post.isPined();
        }
    }

    @Data
    public static class PostDetailCategoryResponse{
        private Long categoryId;
        private String name;

        public PostDetailCategoryResponse(Category category) {
            this.categoryId = category.getMenuId();
            this.name = category.getName();
        }
    }
}
