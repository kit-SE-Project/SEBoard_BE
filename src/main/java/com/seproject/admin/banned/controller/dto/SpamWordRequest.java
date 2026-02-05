package com.seproject.admin.banned.controller.dto;

import lombok.*;

public class SpamWordRequest {
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class SpamWordCreateRequest {
        private String word;
    }
}
