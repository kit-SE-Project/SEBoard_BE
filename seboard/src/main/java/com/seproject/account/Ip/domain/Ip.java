package com.seproject.account.Ip.domain;

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
@Table(name = "ip")
public class Ip {

    @Id @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    private IpType ipType;
}
