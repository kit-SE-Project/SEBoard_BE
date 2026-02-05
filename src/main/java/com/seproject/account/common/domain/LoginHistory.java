package com.seproject.account.common.domain;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Entity
@Table(name = "login_histories")
public class LoginHistory {

    @Id @GeneratedValue
    private Long id;
    private String loginId;
    private LocalDateTime time;

    public LoginHistory(String loginId) {
        this.loginId = loginId;
        this.time = LocalDateTime.now();
    }



}
