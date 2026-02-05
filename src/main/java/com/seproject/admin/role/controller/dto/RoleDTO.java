package com.seproject.admin.role.controller.dto;

import com.seproject.account.role.domain.Role;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class RoleDTO {

    @Data
    @NoArgsConstructor
    public static class RoleResponse {
        private Long accountId;
        private Long roleId;
        private String name;
        private String description;
        private String alias;
        private boolean immutable;

        @Builder(access = AccessLevel.PRIVATE)
        public RoleResponse(Long accountId, Long roleId, String name, String description, String alias,boolean immutable) {
            this.accountId = accountId;
            this.roleId = roleId;
            this.name = name;
            this.description = description;
            this.alias = alias;
            this.immutable = immutable;
        }

        @Builder(access = AccessLevel.PRIVATE)
        public RoleResponse(Long accountId, Long roleId, String name, String description, String alias) {
            this.accountId = accountId;
            this.roleId = roleId;
            this.name = name;
            this.description = description;
            this.alias = alias;
            this.immutable = Role.isImmutable(name);
        }

        public static RoleResponse of(Role role) {
            return builder()
                    .roleId(role.getId())
                    .name(role.getAuthority())
                    .description(role.getDescription())
                    .immutable(role.isImmutable())
                    .alias(role.toString())
                    .build();
        }
    }

    @Data
    public static class CreateRoleRequest {
        private String name;
        private String description;
        private String alias;
    }

    @Data
    public static class UpdateRoleRequest {
        private String name;
        private String description;
        private String alias;
    }

    @Data
    public static class DeleteRoleRequest {
        private Long roleId;
    }

}
