package com.seproject.board.menu.application.dto;

import lombok.Data;

public class CategoryCommand {

    @Data
    public static class CategoryUpdateCommand{
        private Long categoryId;
        private String name;
        private String urlId;
        private String externalUrl;

        public CategoryUpdateCommand(Long categoryId, String name, String urlId, String externalUrl) {
            this.categoryId = categoryId;
            this.name = name;
            this.urlId = urlId;
            this.externalUrl = externalUrl;
        }
    }
}
