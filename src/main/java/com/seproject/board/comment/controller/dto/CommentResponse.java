package com.seproject.board.comment.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seproject.member.controller.dto.UserResponse;
import com.seproject.board.comment.domain.model.Comment;
import com.seproject.board.comment.domain.model.Reply;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class CommentResponse {
    @Data
    public static class RetrieveCommentProfileElement {
        private Long commentId;
        private Long superCommentId;
        private Long tagCommentId;
        private Long postId;
        private UserResponse author;
        private String contents;
        private LocalDateTime createdAt;
        private LocalDateTime modifiedAt;

        public RetrieveCommentProfileElement(Comment comment){
            this.commentId = comment.getCommentId();
            this.postId = comment.getPost().getPostId();
            this.author = new UserResponse(comment.getAuthor());
            this.contents = comment.getContents();
            this.createdAt = comment.getBaseTime().getCreatedAt();
            this.modifiedAt = comment.getBaseTime().getModifiedAt();

            if(comment instanceof Reply){
                Reply reply = (Reply) comment;
                this.superCommentId = reply.getSuperComment().getCommentId();
                this.tagCommentId = reply.getTag().getCommentId();
            }else{
                this.superCommentId = null;
                this.tagCommentId = null;
            }
        }
    }

    @Data
    @Builder(access = AccessLevel.PRIVATE)
    public static class CommentListResponse{
        private PaginationResponse paginationInfo;
        private List<CommentListElement> content;

        public static CommentListResponse toDto(List<CommentListElement> data, PaginationResponse paginationInfo){
            return builder()
                    .content(data)
                    .paginationInfo(paginationInfo)
                    .build();
        }
    }

    @Data
    @Builder(access = AccessLevel.PRIVATE)
    public static class CommentListElement{
        private Long commentId;
        private UserResponse author;
        private LocalDateTime createdAt;
        private LocalDateTime modifiedAt;
        private String contents;
        @JsonProperty("isEditable")
        private boolean isEditable;
        @JsonProperty("isActive")
        private boolean isActive;
        @JsonProperty("isReadOnlyAuthor")
        private boolean isReadOnlyAuthor;
        private List<ReplyResponse> subComments;

        public static CommentListElement toDto(Comment comment, boolean isAuthor, boolean isPostAuthor, List<ReplyResponse> subComments){
            UserResponse userResponse = null;
            String contents = null;
            //TODO : dto에 로직이 들어가는게 맞나?
            if(comment.isDeleted()){
                contents = "삭제된 댓글입니다.";
                userResponse = new UserResponse(null, "(알수 없음)");
            }else if(comment.isOnlyReadByAuthor() && !isAuthor && !isPostAuthor){
                contents = "글 작성자만 볼 수 있는 댓글 입니다.";
                userResponse = new UserResponse(null, "(알수 없음)");
            }else if(comment.isReported()){
                contents = "신고된 댓글입니다.";
                userResponse = new UserResponse(null, "(알수 없음)");
            }else{
                contents = comment.getContents();
                userResponse = new UserResponse(comment.getAuthor());
            }

            return builder()
                    .commentId(comment.getCommentId())
                    .author(userResponse)
                    .createdAt(comment.getBaseTime().getCreatedAt())
                    .modifiedAt(comment.getBaseTime().getModifiedAt())
                    .contents(contents)
                    .isEditable(isAuthor)
                    .isActive(!comment.isReported() && !comment.isDeleted() && ((comment.isOnlyReadByAuthor() && isAuthor) || !comment.isOnlyReadByAuthor()) ) //TODO : 작성자만 읽을 수 있는 경우 추가 필요
                    .isReadOnlyAuthor(comment.isOnlyReadByAuthor())
                    .subComments(subComments)
                    .build();
        }
    }
}
