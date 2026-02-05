package com.seproject.board.post.domain.model.exposeOptions;

import com.seproject.account.account.domain.Account;
import com.seproject.account.role.domain.Role;
import lombok.*;

import javax.persistence.*;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Entity
@Table(name = "expose_options")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "expose_type", discriminatorType = DiscriminatorType.STRING)
public abstract class ExposeOption {
    @Id @GeneratedValue
    private Long exposeOptionId;
    @Enumerated(EnumType.STRING)
    @Column(name = "expose_type", insertable = false, updatable = false)
    protected ExposeState exposeState;
    protected String password;

    public abstract ExposeState getExposeState();

    public boolean pass(List<Role> roles,String password) {
        switch (exposeState){
            case PUBLIC:
                return true;
            case KUMOH:
                List<String> collect = roles.stream()
                        .map(Role::getAuthority)
                        .collect(Collectors.toList());
                return collect.contains(Role.ROLE_KUMOH);
            case PRIVACY:
                return this.password.equals(password);
            default:
                throw new IllegalArgumentException("Invalid ExposeState");
        }

    }


    public static ExposeOption of(ExposeState state, String password){
        switch (state){
            case PUBLIC:
                return new Public();
            case KUMOH:
                return new Kumoh();
            case PRIVACY:
                return new Privacy(password);
            default:
                throw new IllegalArgumentException("Invalid ExposeState");
        }
    }



}
