package com.seproject.account.role.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "roles")
public class Role implements GrantedAuthority {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_KUMOH = "ROLE_KUMOH";

    private static final List<String> IMMUTABLE_ROLES = List.of(ROLE_ADMIN,ROLE_USER,ROLE_KUMOH);
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roleId;

    @Column(nullable=false, unique=true)
    @Getter
    private String name;

    private String description;
    private String alias;

    public static boolean isImmutable(String name) {
        return IMMUTABLE_ROLES.contains(name);
    }

    public Long getId() {
        return roleId;
    }
    @Override
    public String getAuthority() {
        return name;
    }

    public String getDescription() {return description;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(roleId, role.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId);
    }

    @Override
    public String toString() {
        return alias;
    }

    public boolean isImmutable() {
        return IMMUTABLE_ROLES.contains(name);
    }

    public Role update(String name, String description, String alias) {
        this.name = name;
        this.description = description;
        this.alias = alias;

        return this;
    }

}
