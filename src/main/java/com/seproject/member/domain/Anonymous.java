package com.seproject.member.domain;

import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@SuperBuilder
@AllArgsConstructor
@Entity
@Table(name="anonymous")
@PrimaryKeyJoinColumn(name="anonymous_id")
public class Anonymous extends BoardUser {
    @Override
    public String getLoginId(){
        return null; //TODO : UUID
    }

    @Override
    public boolean isAnonymous() {
        return true;
    }
}
