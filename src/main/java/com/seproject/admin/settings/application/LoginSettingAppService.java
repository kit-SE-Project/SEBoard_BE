package com.seproject.admin.settings.application;

import com.seproject.account.account.domain.Account;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.admin.dashboard.domain.DashBoardMenu;
import com.seproject.admin.dashboard.service.AdminDashBoardServiceImpl;
import com.seproject.admin.settings.controller.dto.LoginSettingDTO;
import com.seproject.admin.settings.service.LoginSettingService;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomAccessDeniedException;
import com.seproject.error.exception.CustomAuthenticationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class LoginSettingAppService {

    private final LoginSettingService loginSettingService;
    private final AdminDashBoardServiceImpl dashBoardService;

    private void checkAuthorization() {
        DashBoardMenu dashBoardMenu = dashBoardService.findDashBoardMenuByUrl(DashBoardMenu.ACCOUNT_POLICY_URL);
        Optional<Account> account = SecurityUtils.getAccount();

        if(account.isEmpty()) {
            throw new CustomAuthenticationException(ErrorCode.NOT_LOGIN,null);
        }

        Account userAccount = account.get();
        boolean authorize = dashBoardMenu.authorize(userAccount.getRoles());

        if(!authorize) {
            throw new CustomAccessDeniedException(ErrorCode.ACCESS_DENIED,null);
        }
    }

    public LoginSettingDTO getLoginSetting() {
        checkAuthorization();
        return new LoginSettingDTO(loginSettingService.getLoginLimitTime(), loginSettingService.getLoginTryCount());
    }

    @Transactional
    public void update(LoginSettingDTO request) {
        checkAuthorization();
        loginSettingService.update(request.getLoginLimitTime(), request.getLoginTryCount());
    }

}
