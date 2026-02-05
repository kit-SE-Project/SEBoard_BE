package com.seproject.admin.comment.controller.condition;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AdminCommentRetrieveCondition{
    @JsonProperty("isReported")
    private Boolean isReported;
    @JsonProperty("isReadOnlyAuthor")
    private Boolean isReadOnlyAuthor;
    private String searchOption;
    private String query;
}