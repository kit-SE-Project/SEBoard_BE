package com.seproject.skill.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "skill_tags")
@Getter
@NoArgsConstructor
public class SkillTag {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SkillCategory category;

    @Column(nullable = false)
    private boolean isActive = true;

    private String iconSlug;

    @Builder
    public SkillTag(String name, SkillCategory category, String iconSlug) {
        this.name = name;
        this.category = category;
        this.isActive = true;
        this.iconSlug = iconSlug;
    }

    public void update(String name, SkillCategory category, String iconSlug) {
        this.name = name;
        this.category = category;
        this.iconSlug = iconSlug;
    }

    public void toggleActive() {
        this.isActive = !this.isActive;
    }
}
