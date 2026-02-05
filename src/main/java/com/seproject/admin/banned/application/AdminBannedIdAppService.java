package com.seproject.admin.banned.application;

import com.seproject.account.account.domain.Account;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.admin.banned.controller.dto.BannedIdDTO.BannedIdResponse;
import com.seproject.admin.banned.domain.BannedId;
import com.seproject.admin.banned.persistence.BannedIdQueryRepository;
import com.seproject.admin.banned.service.BannedIdService;
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

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class AdminBannedIdAppService {

    private final BannedIdService bannedIdService;
    private final BannedIdQueryRepository bannedIdQueryRepository;

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

    public Page<BannedIdResponse> findAll(int page, int perPage) {
        checkAuthorization();

        PageRequest pageRequest = PageRequest.of(page, perPage);
        return bannedIdQueryRepository.findAll(pageRequest);
    }

    @Transactional
    public Long create(String bannedId) {
        checkAuthorization();

        Optional<BannedId> byId = bannedIdService.findById(bannedId);

        if(byId.isPresent()) {
            throw new CustomIllegalArgumentException(ErrorCode.ALREADY_EXIST_BANNED_ID,null);
        }

        return bannedIdService.createBannedId(bannedId);
    }

    @Transactional
    public void delete(String bannedId) {
        checkAuthorization();

        bannedIdService.deleteBannedId(bannedId);
    }
}
