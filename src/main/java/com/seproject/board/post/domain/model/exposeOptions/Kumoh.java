package com.seproject.board.post.domain.model.exposeOptions;


import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("KUMOH")
public class Kumoh extends ExposeOption{
    @Override
    public ExposeState getExposeState() {
        return ExposeState.KUMOH;
    }
}
