package com.seproject.admin.settings.service;

import com.seproject.admin.settings.domain.LoginSetting;
import com.seproject.admin.settings.domain.repository.LoginSettingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service
public class LoginSettingService {

    private final LoginSettingRepository loginSettingRepository;

    private LoginSetting loginSetting;

    public LoginSettingService(LoginSettingRepository loginSettingRepository) {
        this.loginSettingRepository = loginSettingRepository;
        Page<LoginSetting> all = loginSettingRepository.findAll(PageRequest.of(0, 1));

        if(all.isEmpty()) {
           loginSetting = new LoginSetting(300,5);
           loginSettingRepository.saveAndFlush(loginSetting);
        } else {
            loginSetting = all.getContent().get(0);
        }
    }

    public long getLoginLimitTime() {
        return loginSetting.getLoginLimitTime();
    }

    public long getLoginTryCount() {
        return loginSetting.getLoginTryCount();
    }

    @Transactional
    public long update(long loginLimitTime, long loginTryCount) {
        int updatedRows = loginSettingRepository.updateAll(loginLimitTime,loginTryCount);
        Page<LoginSetting> all = loginSettingRepository.findAll(PageRequest.of(0, 1));

        if(all.isEmpty()) {
            loginSetting = new LoginSetting(300,5);
            loginSettingRepository.saveAndFlush(loginSetting);
        } else {
            loginSetting = all.getContent().get(0);
        }

        return updatedRows;
    }
}
