package com.seproject.board.bulletin.controller;


import com.seproject.board.bulletin.controller.dto.MainPageDTO.RetrieveMainPageResponse;
import com.seproject.board.bulletin.application.MainPageAppService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "메인 페이지 API", description = "메인 페이지(mainPage) 관련 API")
@RestController
@RequestMapping("/mainPage")
@AllArgsConstructor
public class MainPageController {
    private final MainPageAppService mainPageAppService;
    @GetMapping
    public ResponseEntity<List<RetrieveMainPageResponse>> retrieveMainPagePost
            (@RequestParam(required = false, defaultValue = "10") int size){
        List<RetrieveMainPageResponse> response = mainPageAppService.findMainPagePosts(size);
        return ResponseEntity.ok(response);
    }
}
