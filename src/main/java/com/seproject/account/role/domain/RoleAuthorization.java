package com.seproject.account.role.domain;

import com.seproject.account.authorization.domain.Authorization;
import com.seproject.account.role.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "authorization_metadata")
public class RoleAuthorization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authorization_id")
    private Authorization authorization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    public RoleAuthorization(Authorization authorization, Role role) {
        this.authorization = authorization;
        this.role = role;
    }
}
