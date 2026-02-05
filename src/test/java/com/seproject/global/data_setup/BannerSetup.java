package com.seproject.global.data_setup;

import com.seproject.board.bulletin.domain.model.Banner;
import com.seproject.board.bulletin.domain.repository.BannerRepository;
import com.seproject.file.domain.model.FileMetaData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class BannerSetup {

    @Autowired
    BannerRepository bannerRepository;

    public Banner createActiveBanner(FileMetaData fileMetaData) {
        LocalDate startDate = LocalDate.now().minusDays(2);
        LocalDate endDate = LocalDate.now().plusDays(2);

        String bannerUrl = UUID.randomUUID().toString().substring(0,8);
        return createBanner(startDate,endDate,fileMetaData,bannerUrl);
    }

    public Banner createInActiveBanner(FileMetaData fileMetaData,boolean before) {
        LocalDate startDate = before ? LocalDate.now().minusDays(5) : LocalDate.now().plusDays(2);
        LocalDate endDate = before ? LocalDate.now().minusDays(2) : LocalDate.now().plusDays(5);

        String bannerUrl = UUID.randomUUID().toString().substring(0,8);
        return createBanner(startDate,endDate,fileMetaData,bannerUrl);
    }

    public Banner createBanner(LocalDate startDate , LocalDate endDate, FileMetaData fileMetaData, String bannerUrl) {
        Banner banner = new Banner(startDate, endDate, fileMetaData, bannerUrl);
        bannerRepository.save(banner);
        return banner;
    }
}
