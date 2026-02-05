package com.seproject.admin.menu.controller;

import com.seproject.admin.menu.application.AdminMenuAppService;
import com.seproject.board.common.controller.dto.MessageResponse;
import com.seproject.board.menu.controller.dto.CategoryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.seproject.admin.menu.controller.dto.MenuDTO.*;
import static com.seproject.board.menu.controller.dto.CategoryRequest.*;

@Tag(name = "카테고리 관리 API", description = "카테고리(category) 관리 API")
@RestController
@RequestMapping("/admin/menu")
@AllArgsConstructor
public class AdminMenuController {

    private final AdminMenuAppService adminMenuAppService;

    @Operation(summary = "하위 카테고리 생성", description = "카테고리를 생성한다")
    @PostMapping
    public ResponseEntity<Void> createCategory(
            @RequestBody CreateMenuRequest request,
            @RequestParam String categoryType) {

        /**
         * TODO : jwt
         *         존재하지 않는 상위 카테고리
         *         이미 존재하는 카테고리 이름
         *         권한 없음
         */

        adminMenuAppService.createMenu(request,categoryType);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Parameter(name = "request", description = "관리자가 메뉴(BoardMenu 까지) 정보를 조회")
    @Operation(summary = "관리자가 카테고리 조회", description = "관리자가 카테고리를 조회한다")
    @GetMapping
    public ResponseEntity<List<MenuResponse>> retrieveMenu() {
        List<MenuResponse> response = adminMenuAppService.findAllMenuTree();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "카테고리 정보 상세 조회", description = "카테고리 정보를 상세하게 조회한다.")
    @GetMapping("/{categoryId}")
    public ResponseEntity<MenuResponse> retrieveCategoryInfo(@PathVariable Long categoryId) {
        MenuResponse response = adminMenuAppService.findMenu(categoryId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "카테고리 수정", description = "카테고리를 수정한다")
    @PutMapping("/{categoryId}")
    public ResponseEntity<Void> updateCategory(@PathVariable Long categoryId, @RequestBody UpdateMenuRequest request) {

        /**
         * TODO : jwt
         *    권한 없음
         *    존재하지 않는 상위 카테고리
         *    이미 존재하는 카테고리 이름
         */

        adminMenuAppService.update(categoryId,request);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Parameter(name = "categoryId", description = "삭제할 카테고리 pk")
    @Operation(summary = "카테고리 삭제", description = "소분류 카테고리를 삭제한다.")
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long categoryId) {

        /**
         * TODO : jwt
         *    권한 없음
         *    대분류 삭제시 하위 카테고리 없어야함
         */

        adminMenuAppService.delete(categoryId);
        return new ResponseEntity<>(categoryId, HttpStatus.OK);
    }

    @PostMapping("/migrate")
    public ResponseEntity<Void> migrateCategory(@RequestBody MigrateCategoryRequest request) {
        adminMenuAppService.migrateCategory(request.getFromBoardMenuId(), request.getToBoardMenuId());
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
