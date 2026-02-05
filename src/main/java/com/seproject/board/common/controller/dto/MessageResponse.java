package com.seproject.board.common.controller.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(access = AccessLevel.PRIVATE)
public class MessageResponse {
    private String message;

    public static MessageResponse of(String message) {
        return builder()
                .message(message)
                .build();
    }

    @Data
    @AllArgsConstructor
    public static class CreateAndUpdateMessage {
        private Long id;
        private String message;

        public static CreateAndUpdateMessage of(Long id, String message){
            return new CreateAndUpdateMessage(id, message);
        }
    }
}
