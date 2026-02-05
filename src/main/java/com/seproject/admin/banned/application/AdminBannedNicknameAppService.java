package com.seproject.admin.banned.application;

import com.seproject.account.account.domain.Account;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.admin.banned.controller.dto.BannedNicknameDTO.BannedNicknameResponse;
import com.seproject.admin.banned.domain.BannedNickname;
import com.seproject.admin.banned.persistence.BannedNicknameQueryRepository;
import com.seproject.admin.banned.service.BannedNicknameService;
import com.seproject.admin.dashboard.domain.DashBoardMenu;
import com.seproject.admin.dashboard.service.AdminDashBoardServiceImpl;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomAccessDeniedException;
import com.seproject.error.exception.CustomAuthenticationException;
import com.seproject.error.exception.CustomIllegalArgumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class AdminBannedNicknameAppService {
    private final BannedNicknameService bannedNicknameService;

    private final BannedNicknameQueryRepository bannedNicknameQueryRepository;

    private final AdminDashBoardServiceImpl dashBoardService;

    //TODO : 추후 AOP로 변경 필요
    private void checkAuthorization(){
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN, null));

        DashBoardMenu dashboardmenu = dashBoardService.findDashBoardMenuByUrl(DashBoardMenu.ACCOUNT_POLICY_URL);

        if(!dashboardmenu.authorize(account.getRoles())){
            throw new CustomAccessDeniedException(ErrorCode.ACCESS_DENIED, null);
        }
    }
    public Page<BannedNicknameResponse> findAll(int page,int perPage) {
        checkAuthorization();

        PageRequest pageRequest = PageRequest.of(page, perPage);
        return bannedNicknameQueryRepository.findAll(pageRequest);
    }

    @Transactional
    public Long createBannedNickname(String nickname) {
        checkAuthorization();

        Optional<BannedNickname> byNickname = bannedNicknameService.findByNickname(nickname);
        if (byNickname.isPresent()) {
            throw new CustomIllegalArgumentException(ErrorCode.ALREADY_EXIST_BANNED_NICKNAME,null);
        }

        return bannedNicknameService.createBannedNickname(nickname);
    }

    @Transactional
    public void deleteBannedNickname(String nickname) {
        checkAuthorization();

        bannedNicknameService.deleteBannedNickname(nickname);
    }
}
