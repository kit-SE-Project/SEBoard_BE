package com.seproject.admin.banned.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "banned_nickname")
public class BannedNickname {

    @Id @GeneratedValue
    private Long id;

    @Column(name = "banned_nickname",unique = true)
    private String bannedNickname;

}
