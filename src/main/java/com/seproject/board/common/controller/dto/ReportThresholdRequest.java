package com.seproject.board.common.controller.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ReportThresholdRequest {
    @NotNull
    private Integer postThreshold;
    @NotNull
    private Integer commentThreshold;
}
