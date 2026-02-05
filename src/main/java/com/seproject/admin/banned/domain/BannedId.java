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
@Table(name = "banned_id")
public class BannedId {

    @Id @GeneratedValue
    private Long id;

    @Column(name = "banned_id",unique = true)
    private String bannedId;
}
