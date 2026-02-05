package com.seproject.admin.banned.controller.dto;

import com.seproject.admin.banned.domain.SpamWord;
import lombok.Builder;
import lombok.Data;

@Data
public class SpamWordResponse {
    private Long id;
    private String word;

    public SpamWordResponse(SpamWord spamWord) {
        this.id = spamWord.getId();
        this.word = spamWord.getWord();
    }
}
