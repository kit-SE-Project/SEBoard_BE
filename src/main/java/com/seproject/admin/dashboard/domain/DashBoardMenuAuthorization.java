package com.seproject.admin.dashboard.domain;

import com.seproject.account.role.domain.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;


@Entity
@Getter
@NoArgsConstructor
public class DashBoardMenuAuthorization {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    private DashBoardMenu dashBoardMenu;

    @ManyToOne(fetch = FetchType.EAGER)
    private Role role;

    public DashBoardMenuAuthorization(DashBoardMenu dashBoardMenu, Role role) {
        this.dashBoardMenu = dashBoardMenu;
        this.role = role;
    }

    public boolean authorize(List<Role> roles) {

        for (Role role : roles) {
            if(role.equals(this.role)) {
                return true;
            }
        }

        return false;
    }

}
