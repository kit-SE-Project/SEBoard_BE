package com.seproject.board.post.domain.model.exposeOptions;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("PUBLIC")
public class Public extends ExposeOption {

    @Override
    public ExposeState getExposeState() {
        return ExposeState.PUBLIC;
    }
}
