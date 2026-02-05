package com.seproject.board.post.domain.model.exposeOptions;

import lombok.NoArgsConstructor;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@NoArgsConstructor
@Entity
@DiscriminatorValue("PRIVACY")
public class Privacy extends ExposeOption{

    protected Privacy(String password) {
        this.password = password;
    }

    public boolean checkPassword(String password){
        return this.password.equals(password);
    }

    @Override
    public ExposeState getExposeState() {
        return ExposeState.PRIVACY;
    }
}
