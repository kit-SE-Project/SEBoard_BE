package com.seproject.board.comment.domain.model;

import com.seproject.board.common.BaseTime;
import com.seproject.board.common.domain.ReportThreshold;
import com.seproject.board.common.Status;
import com.seproject.board.post.domain.model.Post;
import com.seproject.member.domain.BoardUser;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;

@Getter
@SuperBuilder
@NoArgsConstructor
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "comment_type")
@DiscriminatorValue("comment")
@Table(name = "comments")
public class Comment {
    private static final int CONTENTS_MIN_SIZE = 1;

    @Id @GeneratedValue
    private Long commentId;

    @Column(columnDefinition = "TEXT")
    private String contents;

    @Embedded
    private BaseTime baseTime;
    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;
    @OneToOne
    @JoinColumn(name = "board_user_id")
    private BoardUser author;
    private boolean isOnlyReadByAuthor;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Status status = Status.NORMAL;
    private int reportCount;

    public Reply writeReply(String contents, Comment taggedComment, BoardUser author, boolean isOnlyReadByAuthor){
        return Reply.builder()
                .post(post)
                .contents(contents)
                .author(author)
                .tag(taggedComment)
                .baseTime(BaseTime.now())
                .isOnlyReadByAuthor(isOnlyReadByAuthor)
                .superComment(this)
                .build();
    }

    public boolean isWrittenBy(Long accountId) {
        return author.isOwnAccountId(accountId);
    }

    public boolean isDeleted(){
        return status == Status.PERMANENT_DELETED || status == Status.TEMP_DELETED;
    }
    public void changeContents(String contents) {
        if(!isValidContents(contents)){
            throw new IllegalArgumentException();
        }

        this.contents = contents;
    }

    public void changeOnlyReadByAuthor(boolean isOnlyReadByAuthor) {
        this.isOnlyReadByAuthor = isOnlyReadByAuthor;
    }


    private boolean isValidContents(String contents) {
        return CONTENTS_MIN_SIZE < contents.length();
    }

    public void delete(boolean isPermanent) {
        if(isPermanent){
            this.status = Status.PERMANENT_DELETED;
        }else{
            this.status = Status.TEMP_DELETED;
        }
    }

    public void increaseReportCount(ReportThreshold reportThreshold){
        this.reportCount++;
        if(reportThreshold.isOverThreshold(reportCount)){
            this.status = Status.REPORTED;
        }
    }

    public void restore() {
        if(status!=Status.NORMAL){
            this.status = Status.NORMAL;
            reportCount = 0;
        }
    }

    public boolean isReported() {
        if(status == Status.REPORTED){
            return true;
        }
        return false;
    }
}
