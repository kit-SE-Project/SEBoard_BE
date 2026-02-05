package com.seproject.member.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;

@Getter
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name="members")
@PrimaryKeyJoinColumn(name="member_id")
public class Member extends BoardUser{


    @Override
    public boolean isAnonymous() {
        return false;
    }
}
