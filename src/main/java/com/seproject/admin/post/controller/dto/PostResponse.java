package com.seproject.admin.post.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seproject.member.controller.dto.UserResponse;
import com.seproject.board.menu.domain.model.Menu;
import com.seproject.board.common.Status;
import com.seproject.board.post.domain.model.Post;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostResponse {
    @Data
    public static class MenuResponse {
        private Long menuId;
        private String name;
        private String urlId;

        public MenuResponse(Menu menu){
            this.menuId = menu.getMenuId();
            this.name = menu.getName();
            this.urlId = menu.getUrlInfo();
        }
    }

    @Data
    public static class PostRetrieveResponse {
        private Long postId;
        private String title;
        private com.seproject.board.post.controller.dto.PostResponse.PostDetailCategoryResponse category;
        private MenuResponse menu;
        private UserResponse author;
        private Integer views;
        private LocalDateTime createdAt;
        private LocalDateTime modifiedAt;
        private boolean hasAttachment;
        @JsonProperty("isReported")
        private boolean isReported;
        private String exposeOption;

        public PostRetrieveResponse(Post post){
            this.postId = post.getPostId();
            this.title = post.getTitle();
            this.category = new com.seproject.board.post.controller.dto.PostResponse.PostDetailCategoryResponse(post.getCategory());
            this.menu = new MenuResponse(post.getCategory().getSuperMenu());
            this.author = new UserResponse(post.getAuthor());
            this.views = post.getViews();
            this.createdAt = post.getBaseTime().getCreatedAt();
            this.modifiedAt = post.getBaseTime().getModifiedAt();
            this.hasAttachment = post.hasAttachments();
            this.isReported = (post.getStatus()== Status.REPORTED);
            this.exposeOption = post.getExposeOption().getExposeState().toString();
        }

    }

    @Data
    public static class DeletedPostResponse {
        private Long postId;
        private String title;
        private com.seproject.board.post.controller.dto.PostResponse.PostDetailCategoryResponse category;
        private MenuResponse menu;
        private UserResponse author;
        private Integer views;
        private LocalDateTime createdAt;
        private LocalDateTime modifiedAt;
        private boolean hasAttachment;
        private String exposeOption;
        public DeletedPostResponse(Post post){
            this.postId = post.getPostId();
            this.title = post.getTitle();
            this.category = new com.seproject.board.post.controller.dto.PostResponse.PostDetailCategoryResponse(post.getCategory());
            this.menu = new MenuResponse(post.getCategory().getSuperMenu());
            this.author = new UserResponse(post.getAuthor());
            this.views = post.getViews();
            this.createdAt = post.getBaseTime().getCreatedAt();
            this.modifiedAt = post.getBaseTime().getModifiedAt();
            this.hasAttachment = post.hasAttachments();
            this.exposeOption = post.getExposeOption().getExposeState().toString();
        }
    }
}
