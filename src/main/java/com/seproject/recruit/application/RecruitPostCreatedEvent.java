package com.seproject.recruit.application;

import com.seproject.recruit.domain.model.RecruitType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class RecruitPostCreatedEvent {
    private final Long postId;
    private final String title;
    private final RecruitType type;
    private final Long authorAccountId;
    private final List<Long> skillIds;
}
