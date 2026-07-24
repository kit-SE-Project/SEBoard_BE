package com.seproject.recruit.domain.model;

import com.seproject.board.post.domain.model.exposeOptions.ExposeOption;
import com.seproject.member.domain.BoardUser;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import com.seproject.skill.domain.SkillTag;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "recruit_posts")
@Getter
@NoArgsConstructor
public class RecruitPost {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitTag tag;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitStatus status = RecruitStatus.ACTIVE;

    @ManyToOne
    @JoinColumn(name = "board_user_id")
    private BoardUser author;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "expose_option_id")
    private ExposeOption exposeOption;

    // RECRUIT 전용
    private Integer headcount;
    private LocalDate startDate;
    private LocalDate endDate;

    // SEEK 전용
    private String portfolioUrl;

    @OneToMany(mappedBy = "recruitPost", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecruitPostSkillTag> skills = new ArrayList<>();

    @Column(nullable = false)
    private long viewCount = 0;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime modifiedAt;

    @Builder
    public RecruitPost(RecruitType type, RecruitTag tag, String title, String contents,
                        BoardUser author, ExposeOption exposeOption,
                        Integer headcount, LocalDate startDate, LocalDate endDate,
                        String portfolioUrl, List<SkillTag> skills) {
        this.type = type;
        this.tag = tag;
        this.title = title;
        this.contents = contents;
        this.author = author;
        this.exposeOption = exposeOption;
        this.headcount = headcount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.portfolioUrl = portfolioUrl;
        this.status = RecruitStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        if (skills != null) {
            skills.forEach(tag2 -> this.skills.add(new RecruitPostSkillTag(this, tag2)));
        }
    }

    public List<SkillTag> getSkillTags() {
        return skills.stream()
                .map(RecruitPostSkillTag::getSkillTag)
                .collect(Collectors.toList());
    }

    public void update(String title, String contents, RecruitTag tag,
                       Integer headcount, LocalDate startDate, LocalDate endDate,
                       String portfolioUrl, List<SkillTag> newSkillTags, ExposeOption exposeOption) {
        this.title = title;
        this.contents = contents;
        this.tag = tag;
        this.headcount = headcount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.portfolioUrl = portfolioUrl;
        this.exposeOption = exposeOption;
        this.modifiedAt = LocalDateTime.now();
        this.skills.clear();
        newSkillTags.forEach(skillTag -> this.skills.add(new RecruitPostSkillTag(this, skillTag)));
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void close() {
        this.status = RecruitStatus.CLOSED;
    }

    public void softDelete() {
        this.status = RecruitStatus.DELETED;
    }

    public boolean isWrittenBy(Long accountId) {
        return author.isOwnAccountId(accountId);
    }
}
