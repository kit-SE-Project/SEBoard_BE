package com.seproject.member.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "frames")
public class Frame {

    @Id @GeneratedValue
    @Column(name = "frame_id")
    private Long frameId;

    private String name;

    private String description;

    @Column(name = "gradient_start")
    private String gradientStart;

    @Column(name = "gradient_end")
    private String gradientEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "frame_type")
    private FrameType frameType;

    @Builder
    public Frame(String name, String description, String gradientStart, String gradientEnd, FrameType frameType) {
        this.name = name;
        this.description = description;
        this.gradientStart = gradientStart;
        this.gradientEnd = gradientEnd;
        this.frameType = frameType;
    }
}
