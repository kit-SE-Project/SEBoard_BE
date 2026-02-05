package com.seproject.board.menu.controller.dto;

import lombok.Data;

public class CategoryRequest {
    @Data
    public static class MigrateCategoryRequest {
        private Long fromBoardMenuId;
        private Long toBoardMenuId;
    }

}
