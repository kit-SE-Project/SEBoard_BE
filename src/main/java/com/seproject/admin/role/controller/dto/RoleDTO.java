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
        private String badgeType;
        private Integer badgePriority;

        // QueryDSL Projections.constructor 용
        public RoleResponse(Long accountId, Long roleId, String name, String description, String alias) {
            this.accountId = accountId;
            this.roleId = roleId;
            this.name = name;
            this.description = description;
            this.alias = alias;
        }

        public static RoleResponse of(Role role) {
            RoleResponse res = new RoleResponse();
            res.roleId = role.getId();
            res.name = role.getAuthority();
            res.description = role.getDescription();
            res.alias = role.toString();
            res.immutable = role.isImmutable();
            res.badgeType = role.getBadgeType();
            res.badgePriority = role.getBadgePriority();
            return res;
        }
    }

    @Data
    public static class CreateRoleRequest {
        private String name;
        private String description;
        private String alias;
        private String badgeType;
        private Integer badgePriority;
    }

    @Data
    public static class UpdateRoleRequest {
        private String name;
        private String description;
        private String alias;
        private String badgeType;
        private Integer badgePriority;
    }

    @Data
    public static class DeleteRoleRequest {
        private Long roleId;
    }

}
