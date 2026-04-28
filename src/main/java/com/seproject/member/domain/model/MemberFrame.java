package com.seproject.member.domain.model;

import com.seproject.member.domain.Member;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "member_frames")
public class MemberFrame {

    @Id @GeneratedValue
    @Column(name = "member_frame_id")
    private Long memberFrameId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "frame_id")
    private Frame frame;

    @Column(name = "acquired_at")
    private LocalDateTime acquiredAt;

    @Builder
    public MemberFrame(Member member, Frame frame) {
        this.member = member;
        this.frame = frame;
        this.acquiredAt = LocalDateTime.now();
    }
}
