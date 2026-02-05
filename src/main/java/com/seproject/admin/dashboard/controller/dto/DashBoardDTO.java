package com.seproject.admin.dashboard.controller.dto;

import com.seproject.account.role.domain.Role;
import com.seproject.admin.dashboard.domain.DashBoardMenu;
import com.seproject.admin.dashboard.domain.DashBoardMenuAuthorization;
import com.seproject.admin.dashboard.domain.DashBoardMenuGroup;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.seproject.admin.menu.controller.dto.MenuDTO.*;

public class DashBoardDTO {

    @Data
    @Builder(access = AccessLevel.PRIVATE)
    public static class DashBoardMenuResponseElement {

        private Long id;
        private String name;
        private String url;

        public static DashBoardMenuResponseElement toDTO(DashBoardMenu dashBoardMenu) {
            return builder()
                    .id(dashBoardMenu.getId())
                    .name(dashBoardMenu.getName())
                    .url(dashBoardMenu.getUrl())
                    .build();
        }
    }


    @Data
    @Builder(access = AccessLevel.PRIVATE)
    public static class DashBoardMenuResponse {

        private List<DashBoardMenuResponseElement> menu;
        private List<DashBoardMenuResponseElement> person;
        private List<DashBoardMenuResponseElement> content;
        private List<DashBoardMenuResponseElement> setting;

        public static DashBoardMenuResponse toDTO(List<DashBoardMenu> dashBoardMenus) {
            List<DashBoardMenuResponseElement> menu = new ArrayList<>();
            List<DashBoardMenuResponseElement> person = new ArrayList<>();
            List<DashBoardMenuResponseElement> content = new ArrayList<>();
            List<DashBoardMenuResponseElement> setting = new ArrayList<>();

            for (DashBoardMenu dashBoardMenu : dashBoardMenus) {
                List<DashBoardMenuResponseElement> target;

                if(dashBoardMenu.getMenuGroup().equals(DashBoardMenuGroup.MENU_GROUP)) {
                    target = menu;
                } else if(dashBoardMenu.getMenuGroup().equals(DashBoardMenuGroup.PERSON_GROUP)) {
                    target = person;
                } else if(dashBoardMenu.getMenuGroup().equals(DashBoardMenuGroup.CONTENT_GROUP)) {
                    target = content;
                } else {
                    target = setting;
                }

                target.add(DashBoardMenuResponseElement.toDTO(dashBoardMenu));
            }

            return builder()
                    .menu(menu)
                    .person(person)
                    .content(content)
                    .setting(setting)
                    .build();
        }
    }

    @Data
    @Builder(access = AccessLevel.PRIVATE)
    public static class DashBoardMenuAuthorizationResponseElement {

        private DashBoardMenuResponseElement menu;
        private MenuAuthResponse option;

        public static DashBoardMenuAuthorizationResponseElement toDTO(DashBoardMenu dashBoardMenu) {
            List<String> roles = dashBoardMenu.getDashBoardMenuAuthorizations()
                            .stream().map(DashBoardMenuAuthorization::getRole)
                            .map(Role::toString)
                            .collect(Collectors.toList());
            return builder()
                    .menu(DashBoardMenuResponseElement.toDTO(dashBoardMenu))
                    .option(new MenuAuthResponse(dashBoardMenu.getSelectOption().getName(), roles))
                    .build();
        }
    }

    @Data
    @Builder(access = AccessLevel.PRIVATE)
    public static class DashBoardMenuAuthorizationResponse {

        private List<DashBoardMenuAuthorizationResponseElement> menu;
        private List<DashBoardMenuAuthorizationResponseElement> person;
        private List<DashBoardMenuAuthorizationResponseElement> content;
        private List<DashBoardMenuAuthorizationResponseElement> setting;

        public static DashBoardMenuAuthorizationResponse toDTO(List<DashBoardMenu> dashBoardMenus) {
            List<DashBoardMenuAuthorizationResponseElement> menu = new ArrayList<>();
            List<DashBoardMenuAuthorizationResponseElement> person = new ArrayList<>();
            List<DashBoardMenuAuthorizationResponseElement> content = new ArrayList<>();
            List<DashBoardMenuAuthorizationResponseElement> setting = new ArrayList<>();

            for (DashBoardMenu dashBoardMenu : dashBoardMenus) {
                List<DashBoardMenuAuthorizationResponseElement> target;

                if(dashBoardMenu.getMenuGroup().equals(DashBoardMenuGroup.MENU_GROUP)) {
                    target = menu;
                } else if(dashBoardMenu.getMenuGroup().equals(DashBoardMenuGroup.PERSON_GROUP)) {
                    target = person;
                } else if(dashBoardMenu.getMenuGroup().equals(DashBoardMenuGroup.CONTENT_GROUP)) {
                    target = content;
                } else {
                    target = setting;
                }

                target.add(DashBoardMenuAuthorizationResponseElement.toDTO(dashBoardMenu));
            }

            return builder()
                    .menu(menu)
                    .person(person)
                    .content(content)
                    .setting(setting)
                    .build();
        }
    }


    @Data
    public static class DashBoardUpdateRequest {
        private List<DashBoardUpdateRequestElement> menus;

    }


    @Data
    public static class DashBoardUpdateRequestElement {
        private Long id;
        private MenuAuthOption option;
    }

}
