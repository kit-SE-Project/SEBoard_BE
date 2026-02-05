package com.seproject.board.common.domain;

import lombok.NoArgsConstructor;

import javax.persistence.*;


@Entity
@NoArgsConstructor
@Table(name = "reports")
public class Report {
    @Id @GeneratedValue
    private Long reportId;
    private Long targetId; //post or comment pk
    private Long memberId;
    @Enumerated(EnumType.STRING)
    private ReportType reportType;

    private Report(Long targetId, Long memberId, String reportType){
        this.targetId = targetId;
        this.memberId = memberId;
        this.reportType = ReportType.valueOf(reportType);
    }

    public static Report of(Long targetId, Long memberId, String reportType){
        return new Report(targetId, memberId, reportType);
    }
}
