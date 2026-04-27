package com.seproject.board.comment.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seproject.file.controller.dto.FileMetaDataResponse.FileMetaDataElement;
import com.seproject.member.controller.dto.UserResponse;
import com.seproject.board.comment.domain.model.Reply;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Builder(access = AccessLevel.PRIVATE)
public class ReplyResponse {
    private Long commentId;
    private Long tag;
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
    private int likeCount;
    private int dislikeCount;
    private String myReaction;
    private List<FileMetaDataElement> attachments;

    public static ReplyResponse toDto(Reply reply, boolean isAuthor, boolean isPostAuthor,
                                      int likeCount, int dislikeCount, String myReaction,
                                      List<FileMetaDataElement> attachments){
        UserResponse userResponse = null;
        String contents = null;
        //TODO : dto에 로직이 들어가는게 맞나?

        if(reply.isDeleted()){
            contents = "삭제된 댓글입니다.";
            userResponse = new UserResponse(null, "(알수 없음)");
        }else if(reply.isOnlyReadByAuthor() && !isAuthor && !isPostAuthor){
            contents = "글 작성자만 볼 수 있는 댓글 입니다.";
            userResponse = new UserResponse(null, "(알수 없음)");
        }else if(reply.isReported()){
            contents = "신고된 댓글입니다.";
            userResponse = new UserResponse(null, "(알수 없음)");
        }else{
            contents = reply.getContents();
            userResponse = new UserResponse(reply.getAuthor());
        }

        boolean isVisible = !reply.isDeleted() && !reply.isReported()
                && (!reply.isOnlyReadByAuthor() || isAuthor || isPostAuthor);

        return builder()
                .commentId(reply.getCommentId())
                .tag(reply.getTag().getCommentId())
                .author(userResponse)
                .createdAt(reply.getBaseTime().getCreatedAt())
                .modifiedAt(reply.getBaseTime().getModifiedAt())
                .contents(contents)
                .isEditable(isAuthor)
                .isActive(!reply.isReported() && !reply.isDeleted() && ((reply.isOnlyReadByAuthor() && isAuthor) || !reply.isOnlyReadByAuthor())) //TODO : 작성자만 읽을 수 있는 경우 추가 필요
                .isReadOnlyAuthor(reply.isOnlyReadByAuthor())
                .likeCount(likeCount)
                .dislikeCount(dislikeCount)
                .myReaction(myReaction)
                .attachments(isVisible && attachments != null ? attachments : Collections.emptyList())
                .build();
    }
}
