package com.seproject.admin.bulletin.controller.dto;

import com.seproject.board.bulletin.domain.model.MainPageMenu;
import com.seproject.board.menu.domain.model.InternalSiteMenu;
import com.seproject.board.menu.domain.model.Menu;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;

public class MainPageDTO {

    @Data
    @Builder(access = AccessLevel.PRIVATE)
    public static class MainPageMenuResponse {
        private Long id;
        private Long menuId;
        private String name;
        private String url;
        private String description;

        public static MainPageMenuResponse toDTO(MainPageMenu mainPageMenu) {
            Menu menu = mainPageMenu.getMenu();
            return builder()
                    .id(mainPageMenu.getId())
                    .menuId(menu.getMenuId())
                    .name(menu.getName())
                    .url(menu.getUrlInfo())
                    .description(menu.getDescription())
                    .build();
        }
    }

    @Data
    @Builder(access = AccessLevel.PRIVATE)
    public static class InternalSiteMenuResponse {
        private Long categoryId;
        private String name;
        private String description;
        private String url;

        public static InternalSiteMenuResponse toDTO(InternalSiteMenu internalSiteMenu) {

            return builder()
                    .categoryId(internalSiteMenu.getMenuId())
                    .description(internalSiteMenu.getDescription())
                    .name(internalSiteMenu.getName())
                    .url(internalSiteMenu.getUrlInfo())
                    .build();
        }
    }

    @Data
    public static class CreateMainPageMenuRequest {
        private Long menuId;
    }

    @Data
    public static class DeleteMainPageMenuRequest {
        private Long id;
    }

    @Data
    public static class UpdateMainPageMenuRequest {
        private List<Long> menuIds;
    }

}
