package com.seproject.recruit.domain.model;

import com.seproject.skill.domain.SkillTag;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "recruit_post_skills")
@Getter
@NoArgsConstructor
public class RecruitPostSkillTag {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruit_post_id", nullable = false)
    private RecruitPost recruitPost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_tag_id", nullable = false)
    private SkillTag skillTag;

    public RecruitPostSkillTag(RecruitPost recruitPost, SkillTag skillTag) {
        this.recruitPost = recruitPost;
        this.skillTag = skillTag;
    }
}
