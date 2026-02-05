package com.seproject.admin.banned.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SpamWord {
    @Id @GeneratedValue(strategy = javax.persistence.GenerationType.IDENTITY)
    private Long id;
    private String word;

    public SpamWord(String word){
        this.word = word;
    }

    public static SpamWord of(String word) {
        return new SpamWord(word);
    }
}
