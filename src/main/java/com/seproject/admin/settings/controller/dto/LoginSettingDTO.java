package com.seproject.admin.settings.controller.dto;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginSettingDTO {

    private Long loginLimitTime;
    private Long loginTryCount;

    public LoginSettingDTO(long loginLimitTime, long loginTryCount) {
        this.loginLimitTime = loginLimitTime;
        this.loginTryCount = loginTryCount;
    }
}
