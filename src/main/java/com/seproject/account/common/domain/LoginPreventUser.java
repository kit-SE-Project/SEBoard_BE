package com.seproject.account.common.domain;


import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "login_prevent_user")
public class LoginPreventUser {

    @Id @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String loginId;

    private LocalDateTime localDateTime;

    public LoginPreventUser(String loginId, long time){
        this.loginId = loginId;
        this.localDateTime = LocalDateTime.now().plusSeconds(time);
    }
}
