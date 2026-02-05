package com.seproject.board.post.controller.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
public class ExposeOptionRequest {
    @NotBlank
    private String name;
    private String password = "";

    public ExposeOptionRequest(String name) {
        this.name = name;
        this.password = "";
    }

    public ExposeOptionRequest(String name, String password) {
        this.name = name;
        this.password = password;
    }
}
