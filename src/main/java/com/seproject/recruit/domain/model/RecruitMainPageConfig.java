package com.seproject.recruit.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "recruit_main_page_config")
@Getter
@NoArgsConstructor
public class RecruitMainPageConfig {

    @Id
    private Long id = 1L;

    private boolean visible = true;

    private int displayCount = 6;

    @Enumerated(EnumType.STRING)
    private SortType sortType = SortType.LATEST;

    public enum SortType { LATEST, DEADLINE }

    public void update(boolean visible, int displayCount, SortType sortType) {
        this.visible = visible;
        this.displayCount = displayCount;
        this.sortType = sortType;
    }

    public static RecruitMainPageConfig defaultConfig() {
        RecruitMainPageConfig config = new RecruitMainPageConfig();
        config.visible = true;
        config.displayCount = 6;
        config.sortType = SortType.LATEST;
        return config;
    }
}
