package com.seproject.member.domain;

import com.seproject.member.domain.model.Frame;
import com.seproject.member.domain.model.Tier;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name="members")
@PrimaryKeyJoinColumn(name="member_id")
public class Member extends BoardUser{

    @Enumerated(EnumType.STRING)
    @Column(name = "tier")
    private Tier tier = Tier.BRONZE;

    @Column(name = "activity_score")
    private Long activityScore = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipped_frame_id")
    private Frame equippedFrame;

    public void updateTierAndScore(long score) {
        this.activityScore = score;
        this.tier = Tier.of(score);
    }

    public void equipFrame(Frame frame) {
        this.equippedFrame = frame;
    }

    public void unequipFrame() {
        this.equippedFrame = null;
    }

    @Override
    public boolean isAnonymous() {
        return false;
    }
}
