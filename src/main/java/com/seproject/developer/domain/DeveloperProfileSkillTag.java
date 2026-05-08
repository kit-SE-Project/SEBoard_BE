package com.seproject.developer.domain;

import com.seproject.skill.domain.SkillTag;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "developer_profile_skills")
@Getter
@NoArgsConstructor
public class DeveloperProfileSkillTag {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "developer_profile_id", nullable = false)
    private DeveloperProfile developerProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_tag_id", nullable = false)
    private SkillTag skillTag;

    public DeveloperProfileSkillTag(DeveloperProfile developerProfile, SkillTag skillTag) {
        this.developerProfile = developerProfile;
        this.skillTag = skillTag;
    }
}
