package com.seproject.seboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaginationResponse {
    private int contentSize;
    private int currentPage;
    private int lastPage;
    private int perPage;
}
