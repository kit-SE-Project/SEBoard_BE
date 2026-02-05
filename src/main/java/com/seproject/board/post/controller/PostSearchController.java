package com.seproject.board.post.controller;

import com.seproject.board.post.application.PostSearchAppService;
import com.seproject.board.post.controller.dto.PostSearchRequest;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.seproject.board.post.controller.dto.PostResponse.*;

@RestController
@RequestMapping("/search/posts")
@AllArgsConstructor
public class PostSearchController {
    private final PostSearchAppService postSearchAppService;

    @GetMapping
    public ResponseEntity searchPosts(@ModelAttribute PostSearchRequest request,
                                      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int perPage) {
        return ResponseEntity.ok().body(postSearchAppService.searchPost(request, page, perPage));
    }
}
