package com.seproject.account.authorization.domain;

import com.seproject.account.role.domain.Role;
import com.seproject.account.role.domain.RoleAuthorization;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.*;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@DiscriminatorColumn(name = "dtype")
@Inheritance(strategy = InheritanceType.JOINED)
@Entity
@Table(name = "authorizations")
public abstract class Authorization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "authorization", cascade = CascadeType.ALL, orphanRemoval = true)
    protected List<RoleAuthorization> roleAuthorizations = new ArrayList<>();

    public Authorization(List<RoleAuthorization> roleAuthorizations) {
        this.roleAuthorizations = roleAuthorizations;
    }

    public void update(List<Role> roles) {
        List<RoleAuthorization> collect = roles.stream()
                .map((role) -> new RoleAuthorization(this, role))
                .collect(Collectors.toList());
        roleAuthorizations.clear();
        roleAuthorizations.addAll(collect);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Authorization that = (Authorization) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
