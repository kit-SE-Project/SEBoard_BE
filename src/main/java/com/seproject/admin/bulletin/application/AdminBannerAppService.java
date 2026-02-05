package com.seproject.admin.bulletin.application;

import com.seproject.account.account.domain.Account;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.admin.bulletin.controller.dto.BannerDTO.BannerResponse;
import com.seproject.admin.bulletin.persistence.AdminBannerQueryRepository;
import com.seproject.admin.dashboard.domain.DashBoardMenu;
import com.seproject.admin.dashboard.service.AdminDashBoardServiceImpl;
import com.seproject.board.bulletin.service.BannerService;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomAccessDeniedException;
import com.seproject.error.exception.CustomAuthenticationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static com.seproject.admin.bulletin.controller.dto.BannerDTO.CreateBannerRequest;
import static com.seproject.admin.bulletin.controller.dto.BannerDTO.UpdateBannerRequest;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBannerAppService {
    private final BannerService bannerService;
    private final AdminBannerQueryRepository adminBannerQueryRepository;

    private final AdminDashBoardServiceImpl dashBoardService;

    //TODO : 추후 AOP로 변경 필요
    private void checkAuthorization(){
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN, null));

        DashBoardMenu dashboardmenu = dashBoardService.findDashBoardMenuByUrl(DashBoardMenu.MAIN_PAGE_MENU_MANAGE_URL);

        if(!dashboardmenu.authorize(account.getRoles())){
            throw new CustomAccessDeniedException(ErrorCode.ACCESS_DENIED, null);
        }
    }

    public List<BannerResponse> retrieveBanner( Boolean isActive){
        checkAuthorization();

        if(isActive==null){
            return adminBannerQueryRepository.findAllBanner();
        }else if(isActive){
            return adminBannerQueryRepository.findActiveBanners(LocalDate.now());
        }else{
            return adminBannerQueryRepository.findUnActiveBanners(LocalDate.now());
        }
    }

    @Transactional
    public Long createBanner(CreateBannerRequest request){
        checkAuthorization();

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        String bannerUrl = request.getBannerUrl();
        Long fileMetaDataId = request.getFileMetaDataId();

        Long bannerId = bannerService.createBanner(startDate, endDate, bannerUrl, fileMetaDataId);
        return bannerId;
    }

    @Transactional
    public void updateBanner(Long bannerId, UpdateBannerRequest request){
        checkAuthorization();

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        String bannerUrl = request.getBannerUrl();
        Long fileMetaDataId = request.getFileMetaDataId();

        bannerService.updateBanner(bannerId,startDate,endDate,bannerUrl,fileMetaDataId);
    }

    @Transactional
    public void deleteBanner(Long bannerId){
        checkAuthorization();

        bannerService.deleteBanner(bannerId);
    }

}
