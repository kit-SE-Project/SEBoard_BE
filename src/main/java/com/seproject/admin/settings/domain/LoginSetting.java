package com.seproject.admin.settings.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Getter
@NoArgsConstructor
@Entity
public class LoginSetting {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long loginLimitTime;
    private Long loginTryCount;

    public LoginSetting(long loginLimitTime , long loginTryCount) {
        this.loginLimitTime = loginLimitTime;
        this.loginTryCount = loginTryCount;
    }
}
