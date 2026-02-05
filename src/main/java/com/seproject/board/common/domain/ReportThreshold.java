package com.seproject.board.common.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "report_thresholds")
public class ReportThreshold {
    @Id @GeneratedValue
    private Long thresholdId;
    private int threshold;
    @Enumerated(EnumType.STRING)
    private ReportType thresholdType;

    public static ReportThreshold of(int threshold, String thresholdType){
        return new ReportThreshold(threshold, thresholdType);
    }
    public static ReportThreshold of(int threshold, ReportType type){
        return new ReportThreshold(threshold, type);
    }

    private ReportThreshold(int threshold, String thresholdType){
        this.threshold = threshold;
        this.thresholdType = ReportType.valueOf(thresholdType);
    }

    private ReportThreshold(int threshold, ReportType thresholdType){
        this.threshold = threshold;
        this.thresholdType = thresholdType;
    }

    public boolean isOverThreshold(int reportCount){
        return reportCount >= threshold;
    }

    public void setThreshold(int threshold){
        this.threshold = threshold;
    }
}
