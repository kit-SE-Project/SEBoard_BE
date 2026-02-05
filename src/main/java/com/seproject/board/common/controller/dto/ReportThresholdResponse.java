package com.seproject.board.common.controller.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReportThresholdResponse {
    private Integer postThreshold;
    private Integer commentThreshold;

    public static ReportThresholdResponse of(int postThreshold, int commentThreshold){
        return ReportThresholdResponse.builder()
                .postThreshold(postThreshold)
                .commentThreshold(commentThreshold)
                .build();
    }
}
