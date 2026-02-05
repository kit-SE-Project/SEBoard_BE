package com.seproject.member.controller.dto;

import com.seproject.member.domain.BoardUser;
import lombok.Data;

@Data
public class UserResponse {

    private Long userId;
    private String name;

    public UserResponse(Long userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public UserResponse(BoardUser boardUser) {
        if(!boardUser.isAnonymous()){
            this.userId = boardUser.getBoardUserId();
        }
        this.name = boardUser.getName();
    }
}
