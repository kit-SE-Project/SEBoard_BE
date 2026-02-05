package com.seproject.board.bulletin.service;

import com.seproject.board.bulletin.domain.model.Banner;
import com.seproject.board.bulletin.domain.repository.BannerRepository;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.NoSuchResourceException;
import com.seproject.file.application.FileAppService;
import com.seproject.file.domain.model.FileMetaData;
import com.seproject.file.domain.repository.FileMetaDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BannerService {

    private final BannerRepository bannerRepository;
    private final FileMetaDataRepository fileMetaDataRepository;
    private final FileAppService fileAppService; // TODO :

    @Transactional
    public Long createBanner(LocalDate startDate, LocalDate endDate, String bannerUrl, Long fileMetaDataId) {
        FileMetaData fileMetaData = fileMetaDataRepository.findById(fileMetaDataId)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_FILE));

        Banner banner = new Banner(startDate, endDate, fileMetaData, bannerUrl);

        bannerRepository.save(banner);

        return banner.getBannerId();
    }

    @Transactional
    public void updateBanner(Long bannerId,
                             LocalDate startDate,
                             LocalDate endDate,
                             String bannerUrl,
                             Long fileMetaDataId) {

        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_BANNER));

        Long bannerFileMetaDataId = banner.getFileMetaData().getFileMetaDataId();

        if(!bannerFileMetaDataId.equals(fileMetaDataId)){ //TODO :
            fileAppService.deleteFile(bannerFileMetaDataId);
        }

        FileMetaData fileMetaData = fileMetaDataRepository.findById(fileMetaDataId)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_FILE));

        banner.updateBanner(startDate, endDate, fileMetaData, bannerUrl);
    }

    @Transactional
    public void deleteBanner(Long bannerId) {
        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_BANNER));

        fileAppService.deleteFile(banner.getFileMetaData().getFileMetaDataId());
        bannerRepository.delete(banner);
    }

    public List<Banner> findAll() {
        return bannerRepository.findAll();
    }

    public Banner findById(Long bannerId) {
        return bannerRepository.findById(bannerId)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_BANNER));
    }



}
