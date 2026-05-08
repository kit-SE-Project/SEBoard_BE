package com.seproject.recruit.domain.model;

import com.seproject.member.domain.BoardUser;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recruit_comments")
@Getter
@NoArgsConstructor
public class RecruitComment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruit_post_id", nullable = false)
    private RecruitPost recruitPost;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "board_user_id", nullable = false)
    private BoardUser author;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contents;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private RecruitComment parentComment;

    @Column(name = "tag_comment_id")
    private Long tagCommentId;

    private boolean isReadOnlyAuthor = false;

    private boolean isDeleted = false;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime modifiedAt;

    @Builder
    public RecruitComment(RecruitPost recruitPost, BoardUser author, String contents,
                           RecruitComment parentComment, Long tagCommentId, boolean isReadOnlyAuthor) {
        this.recruitPost = recruitPost;
        this.author = author;
        this.contents = contents;
        this.parentComment = parentComment;
        this.tagCommentId = tagCommentId;
        this.isReadOnlyAuthor = isReadOnlyAuthor;
        this.createdAt = LocalDateTime.now();
        this.modifiedAt = LocalDateTime.now();
    }

    public void update(String contents, boolean isReadOnlyAuthor) {
        this.contents = contents;
        this.isReadOnlyAuthor = isReadOnlyAuthor;
        this.modifiedAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.isDeleted = true;
    }

    public boolean isOwner(Long accountId) {
        return this.author.isOwnAccountId(accountId);
    }
}
