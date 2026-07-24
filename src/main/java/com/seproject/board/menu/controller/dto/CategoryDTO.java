package com.seproject.board.menu.controller.dto;

import com.seproject.board.menu.domain.model.Menu;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

public class CategoryDTO {

    @Data
    @Builder(access = AccessLevel.PRIVATE)
    public static class CategoryResponseElement {
        private String name;
        private Long menuId;
        private String urlId;

        //TODO : Access, Manage, Edit, Expose 추가

        public static CategoryResponseElement toDTO(Menu menu) {
            return builder()
                    .name(menu.getName())
                    .menuId(menu.getMenuId())
                    .urlId(menu.getUrlInfo())
                    .build();
        }
    }


}
