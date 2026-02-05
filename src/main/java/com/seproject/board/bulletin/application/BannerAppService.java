package com.seproject.board.bulletin.application;

import com.seproject.board.bulletin.controller.dto.BannerDTO;
import com.seproject.board.bulletin.persistence.BannerQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static com.seproject.board.bulletin.controller.dto.BannerDTO.*;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class BannerAppService {
    private final BannerQueryRepository bannerQueryRepository;

    public List<BannerResponse> findActiveBanners() {
        LocalDate currentDate = LocalDate.now();
        List<BannerResponse> mainPageBanner = bannerQueryRepository.findBanner(currentDate);
        return mainPageBanner;
    }


}
