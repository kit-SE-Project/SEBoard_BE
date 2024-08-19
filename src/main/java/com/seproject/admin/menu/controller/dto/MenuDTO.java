package com.seproject.admin.menu.controller.dto;

import com.seproject.account.authorization.utils.AuthorizationProperty;
import com.seproject.board.menu.domain.model.Menu;
import lombok.Data;
import org.springframework.data.util.Pair;

import java.util.List;
import java.util.Map;

import static com.seproject.account.authorization.utils.AuthorizationProperty.*;

public class MenuDTO {

    @Data
    public static class MenuResponse {
        private Long menuId;
        private String name;
        private String urlId;
        private String type;
        private MenuAuthResponse access;
        private MenuAuthResponse write;
        private MenuAuthResponse manage;
        private MenuAuthResponse expose;
        private List<MenuResponse> subMenus;

        public MenuResponse(Menu menu, List<MenuResponse> subMenus) {
            this.menuId = menu.getMenuId();
            this.name = menu.getName();
            this.urlId = menu.getUrlInfo();
            this.type = menu.getType();
            this.subMenus = subMenus;
        }

        public MenuResponse(Menu menu, List<MenuResponse> subMenus, Map<AuthorizationProperty, Pair<String, List<String>>> authOptions) {
            this.menuId = menu.getMenuId();
            this.name = menu.getName();
            this.urlId = menu.getUrlInfo();
            this.type = menu.getType();
            this.subMenus = subMenus;
            this.access = authOptions.get(ACCESS)!=null ? new MenuAuthResponse(authOptions.get(ACCESS)) : null;
            this.write = authOptions.get(EDITABLE)!=null ? new MenuAuthResponse(authOptions.get(EDITABLE)) : null;
            this.manage = authOptions.get(MANAGEABLE)!=null ? new MenuAuthResponse(authOptions.get(MANAGEABLE)) : null;
            this.expose = authOptions.get(EXPOSE)!=null ? new MenuAuthResponse(authOptions.get(EXPOSE)) : null;
        }
    }

    @Data
    public static class MenuAuthResponse {
        private String option;
        private List<String> roles;

        public MenuAuthResponse(String option, List<String> roles) {
            this.option = option;
            this.roles = roles;
        }

        public MenuAuthResponse(Pair<String, List<String>> authOption) {
            this.option = authOption.getFirst();
            this.roles = authOption.getSecond();
        }
    }

    @Data
    public static class CreateMenuRequest {

        private Long superCategoryId;
        private String name;
        private String description;
        private String urlId;
        private String externalUrl;

        private MenuAuthOption access;
        private MenuAuthOption write;
        private MenuAuthOption expose;
        private MenuAuthOption manage;
    }

    @Data
    public static class UpdateMenuRequest {
        private String name;
        private Long superMenuId;
        private String description;
        private String urlId;
        private String externalUrl;

        private MenuAuthOption access;
        private MenuAuthOption write;
        private MenuAuthOption expose;
        private MenuAuthOption manage;
    }

    @Data
    public static class MenuAuthOption {
        private String option;
        private List<Long> roles;
    }

    @Data
    public static class MigrateMenuRequest{
        private Long toSuperMenuId;
        private Long targetMenuId;
    }

}
