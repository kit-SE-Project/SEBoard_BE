package com.seproject.board.post.controller.dto;

import lombok.Data;

@Data
public class PostSearchRequest {
    private Long categoryId;
    private String searchOption;
    private String query;
}
