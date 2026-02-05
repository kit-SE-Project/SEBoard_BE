package com.seproject.admin.ip.application;

import com.seproject.account.Ip.application.IpService;
import com.seproject.account.Ip.domain.IpType;
import com.seproject.account.account.domain.Account;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.admin.dashboard.domain.DashBoardMenu;
import com.seproject.admin.dashboard.service.AdminDashBoardServiceImpl;
import com.seproject.admin.ip.persitence.IpQueryRepository;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomAccessDeniedException;
import com.seproject.error.exception.CustomAuthenticationException;
import com.seproject.error.exception.CustomIllegalArgumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.seproject.admin.ip.controller.dto.IpDTO.*;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class AdminIpAppService {

    private final IpService ipService;
    private final IpQueryRepository ipQueryRepository;
    private final AdminDashBoardServiceImpl dashBoardService;

    private void checkAuthorization() {
        DashBoardMenu dashBoardMenu = dashBoardService.findDashBoardMenuByUrl(DashBoardMenu.GENERAL_URL);
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

    public List<IpResponse> findAll(IpCondition condition) {
//        PageRequest pageRequest = PageRequest.of(page, perPage);
        checkAuthorization();
        return ipQueryRepository.findAll(condition);

    }

    @Transactional
    public void createIp(CreateIpRequest request) {
        checkAuthorization();
        IpType ipType = IpType.valueOf(request.getIpType());
        ipService.createIp(request.getIpAddress(),ipType);
    }

    @Transactional
    public void delete(DeleteIpRequest request) {
        checkAuthorization();
        ipService.deleteIp(request.getIpAddress());
    }

}
