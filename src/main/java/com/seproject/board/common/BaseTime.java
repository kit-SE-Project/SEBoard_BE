package com.seproject.board.common;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;
import java.time.LocalDateTime;


@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BaseTime {
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    public static BaseTime now(){
        LocalDateTime now = LocalDateTime.now();
        return new BaseTime(now, now);
    }

    public static BaseTime of(LocalDateTime createdAt, LocalDateTime modifiedAt) {
        return new BaseTime(createdAt, modifiedAt);
    }

    public void modify(){
        this.modifiedAt = LocalDateTime.now();
    }

    public boolean idModified() {
        return !createdAt.equals(modifiedAt);
    }
}
