package com.seproject.recruit.service;

import com.seproject.recruit.domain.model.RecruitMainPageConfig;
import com.seproject.recruit.domain.repository.RecruitMainPageConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitMainPageConfigService {

    private final RecruitMainPageConfigRepository configRepository;

    public RecruitMainPageConfig getConfig() {
        return configRepository.findById(1L)
                .orElseGet(() -> configRepository.save(RecruitMainPageConfig.defaultConfig()));
    }

    @Transactional
    public RecruitMainPageConfig update(boolean visible, int displayCount, RecruitMainPageConfig.SortType sortType) {
        RecruitMainPageConfig config = getConfig();
        config.update(visible, displayCount, sortType);
        return config;
    }
}
