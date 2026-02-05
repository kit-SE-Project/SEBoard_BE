package com.seproject.admin.authorization.controller.dto;

public class AuthorizationDTO {

//    @Data
//    public static class AddRoleToCategoryRequest {
//        private Long roleId;
//        private Long categoryId;
//    }
//
//
//    @Data
//    @Builder(access = AccessLevel.PRIVATE)
//    public static class AddRoleToCategoryResponse {
//        private String role;
//        public static AddRoleToCategoryResponse toDTO(Role role) {
//            return builder()
//                    .role(role.getAuthority())
//                    .build();
//        }
//    }
//
//    @Data
//    public static class DeleteRoleToCategoryRequest {
//        private Long roleId;
//        private Long categoryId;
//    }
//
//
//    @Data
//    @Builder(access = AccessLevel.PRIVATE)
//    public static class DeleteRoleToCategoryResponse {
//        private String role;
//        public static DeleteRoleToCategoryResponse toDTO(Role role) {
//            return builder()
//                    .role(role.getAuthority())
//                    .build();
//        }
//    }
//
//    @Data
//    public static class CategoryAccessUpdateRequest {
//        private String name;
//        private String description;
//        private String externalUrl;
//        private String urlId;
//        private CategoryAccessUpdateRequestElement access;
//        private CategoryAccessUpdateRequestElement write;
//        private CategoryAccessUpdateRequestElement manage;
//        private CategoryAccessUpdateRequestElement expose;
//    }
//
//    @Data
//    public static class CategoryAccessUpdateRequestElement {
//        private String option;
//        private List<Long> roles;
//    }
//
//    @Data
//    @Builder(access = AccessLevel.PRIVATE)
//    public static class CategoryAccessOptionResponse {
//        private String name;
//        private String description;
//        private String externalUrl;
//        private String urlId;
//        private AccessResponse access;
//        private MenuAuthorizationResponse write;
//        private MenuAuthorizationResponse manage;
//        private MenuAuthorizationResponse menuExpose;
//
//        public static CategoryAccessOptionResponse toDTO(Menu menu,
//                                                         AccessResponse access,
//                                                         MenuAuthorizationResponse write,
//                                                         MenuAuthorizationResponse manage,
//                                                         MenuAuthorizationResponse menuExpose) {
//
//            return builder()
//                    .name(menu.getName())
//                    .description(menu.getDescription())
//                    .externalUrl(menu.getUrlInfo())
//                    .urlId(menu.getUrlInfo())
//                    .access(access)
//                    .write(write)
//                    .manage(manage)
//                    .menuExpose(menuExpose)
//                    .build();
//        }
//
//
//    }
//
////    @Data
////    @Builder(access = AccessLevel.PRIVATE)
////    public static class MenuAuthorizationResponse {
////        private String option;
////        private List<String> roles;
////
////        public static MenuAuthorizationResponse toDTO(List<MenuAuthorization> menuAuthorizations) {
////            SelectOption selectOption = menuAuthorizations.size() == 0 ? SelectOption.ALL : menuAuthorizations.get(0).getSelectOption();
////
////            List<Role> roles = selectOption.equals(SelectOption.SELECT) ? menuAuthorizations.stream().map(MenuAuthorization::getRole).collect(Collectors.toList())
////                    : List.of();
////            return builder()
////                    .roles(roles.stream().map(Role::toString).collect(Collectors.toList()))
////                    .option(selectOption.getName())
////                    .build();
////        }
////
////    }
//
//    @Data
//    @Builder(access = AccessLevel.PRIVATE)
//    public static class AccessResponse {
//        private String option;
//        private List<String> roles;
//
//        public static AccessResponse toDTO(List<Role> roles,SelectOption selectOption) {
//            List<String> collect = selectOption.equals(SelectOption.SELECT) ?
//                    roles.stream().map(Role::toString).collect(Collectors.toList()) : List.of();
//            return builder()
//                    .option(selectOption.getName())
//                    .roles(collect)
//                    .build();
//        }
//    }
//
//


}
