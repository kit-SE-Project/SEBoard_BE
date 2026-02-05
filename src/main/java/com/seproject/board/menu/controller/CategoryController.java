package com.seproject.board.menu.controller;

import com.seproject.board.menu.application.CategoryAppService;
import com.seproject.board.menu.controller.dto.CategoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "카테고리 API", description = "카테고리(category) 관련 API")
@RestController
@RequestMapping("/menu")
@AllArgsConstructor
public class CategoryController {
    private final CategoryAppService categoryAppService;

    @Operation(summary = "메뉴 목록 조회", description = "권한에 따라서 메뉴 목록을 노출한다.")
    @GetMapping //TODO : 권한별 추가 테스트
    public ResponseEntity<List<CategoryResponse>> retrieveAllMenu(){
        List<CategoryResponse> categoryResponses = categoryAppService.retrieveAllMenu();
        return new ResponseEntity<>(categoryResponses, HttpStatus.OK);
    }

    @Parameter(name = "categoryId", description = "대분류 카테고리 pk")
    @Operation(summary = "하위 카테고리 조회", description = "대분류 카테고리 하위에 있는 모든 소분류 카테고리를 조회한다")
    @GetMapping("/{menuId}")
    public ResponseEntity<CategoryResponse> retrieveMenuById(@PathVariable Long menuId) {

        /**
         * TODO :
         *    존재하지 않는 대분류 카테고리
         */
        CategoryResponse categoryResponse = categoryAppService.retrieveMenuById(menuId);

        return new ResponseEntity<>(categoryResponse, HttpStatus.OK);
    }

}
