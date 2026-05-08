package com.seproject.developer.domain;

import com.seproject.account.account.domain.Account;
import com.seproject.skill.domain.SkillTag;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "developer_profiles")
@Getter
@NoArgsConstructor
public class DeveloperProfile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    private String intro;

    private String githubUrl;

    private String portfolioUrl;

    private String grade;

    @Column(columnDefinition = "TEXT")
    private String readmeContent;

    @OneToMany(mappedBy = "developerProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeveloperProfileSkillTag> skills = new ArrayList<>();

    @Builder
    public DeveloperProfile(Account account, String intro,
                             String githubUrl, String portfolioUrl, String grade,
                             String readmeContent) {
        this.account = account;
        this.intro = intro;
        this.githubUrl = githubUrl;
        this.portfolioUrl = portfolioUrl;
        this.grade = grade;
        this.readmeContent = readmeContent;
    }

    public List<SkillTag> getSkillTags() {
        return skills.stream()
                .map(DeveloperProfileSkillTag::getSkillTag)
                .collect(Collectors.toList());
    }

    public void update(String intro, String githubUrl,
                       String portfolioUrl, String grade, String readmeContent,
                       List<SkillTag> newSkillTags) {
        this.intro = intro;
        this.githubUrl = githubUrl;
        this.portfolioUrl = portfolioUrl;
        this.grade = grade;
        this.readmeContent = readmeContent;
        this.skills.clear();
        newSkillTags.forEach(tag -> this.skills.add(new DeveloperProfileSkillTag(this, tag)));
    }

    public void updateSkills(List<SkillTag> newSkillTags) {
        this.skills.clear();
        newSkillTags.forEach(tag -> this.skills.add(new DeveloperProfileSkillTag(this, tag)));
    }
}
