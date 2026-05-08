package com.seproject.developer.controller;

import com.seproject.developer.domain.DeveloperProfile;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class DeveloperProfileResponse {
    private final Long id;
    private final String intro;
    private final String githubUrl;
    private final String portfolioUrl;
    private final String grade;
    private final String readmeContent;
    private final List<SkillInfo> skills;

    public DeveloperProfileResponse(DeveloperProfile p) {
        this.id = p.getId();
        this.intro = p.getIntro();
        this.githubUrl = p.getGithubUrl();
        this.portfolioUrl = p.getPortfolioUrl();
        this.grade = p.getGrade();
        this.readmeContent = p.getReadmeContent();
        this.skills = p.getSkillTags().stream()
                .map(s -> new SkillInfo(s.getId(), s.getName(), s.getCategory().name(), s.getIconSlug()))
                .collect(Collectors.toList());
    }

    @Data
    public static class SkillInfo {
        private final Long id;
        private final String name;
        private final String category;
        private final String iconSlug;
    }
}
