package com.seproject.admin.controller;

import com.seproject.admin.controller.dto.CategoryDTO;
import com.seproject.admin.dto.AccountDTO;
import com.seproject.seboard.application.CategoryAppService;
import com.seproject.seboard.controller.dto.post.CategoryRequest;
import com.seproject.seboard.controller.dto.post.CategoryResponse;
import com.seproject.seboard.domain.model.category.Menu;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.seproject.seboard.controller.dto.post.CategoryRequest.*;
import static com.seproject.admin.controller.dto.CategoryDTO.*;

@Tag(name = "카테고리 관리 API", description = "카테고리(category) 관리 API")
@RestController
@RequestMapping("/admin/menu")
@AllArgsConstructor
public class AdminCategoryController {

    private final CategoryAppService categoryAppService;

    @Parameter(name = "request", description = "상위 카테고리, 생성할 카테고리 이름 정보")
    @Operation(summary = "하위 카테고리 생성", description = "카테고리를 생성한다")
    @ApiResponses({
            @ApiResponse(content = @Content(schema = @Schema(implementation = CreateCategoryRequest.class)), responseCode = "200", description = "계정 목록 조회 성공"),
    })
    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody CreateCategoryRequest request, @RequestParam String categoryType) {

        /**
         * TODO : jwt
         *         존재하지 않는 상위 카테고리
         *         이미 존재하는 카테고리 이름
         *         권한 없음
         */
        categoryAppService.createCategory(request.toCommand(categoryType));

        return new ResponseEntity<>(request, HttpStatus.OK);
    }

    @Parameter(name = "request", description = "관리자가 메뉴(BoardMenu 까지) 정보를 조회")
    @Operation(summary = "관리자가 카테고리 조회", description = "관리자가 카테고리를 조회한다")
    @ApiResponses({
            @ApiResponse(content = @Content(schema = @Schema(implementation = AdminCategoryRetrieveResponse.class)), responseCode = "200", description = "메뉴 목록을 조회"),
    })
    @GetMapping
    public ResponseEntity<?> retrieveMenu() {

        Map<Menu, List<Menu>> menuListMap = categoryAppService.retrieveAllMenuForAdmin();

        List<CategoryResponse> categoryResponses = new ArrayList<>();
        menuListMap.forEach((menu, subMenus) -> {
            CategoryResponse categoryResponse = new CategoryResponse(menu);
            subMenus.stream()
                    .map(CategoryResponse::new)
                    .forEach(categoryResponse::addSubMenu);

            categoryResponses.add(categoryResponse);
        });

        return new ResponseEntity<>(AdminCategoryRetrieveResponse.toDTO(categoryResponses), HttpStatus.OK);
    }

    @Parameter(name = "request", description = "관리자가 메뉴(BoardMenu 까지) 정보를 조회")
    @Operation(summary = "관리자가 하위 카테고리 목록을 조회", description = "관리자가 하위 카테고리 목록을 조회한다")
    @ApiResponses({
            @ApiResponse(content = @Content(schema = @Schema(implementation = AdminCategoryRetrieveResponse.class)), responseCode = "200", description = "메뉴 목록을 조회"),
    })
    @GetMapping("/{categoryId}/subMenus")
    public ResponseEntity<?> retrieveCategory(@PathVariable Long categoryId) {
        List<Menu> menus = categoryAppService.retrieveCategoryBySuperCategoryId(categoryId);
        return new ResponseEntity<>(SubCategoryRetrieveResponse.toDTO(menus), HttpStatus.OK);
    }

    @Parameters(
            {
                    @Parameter(name = "categoryId", description = "수정할 카테고리 pk"),
                    @Parameter(name = "request", description = "수정할 대분류 카테고리 pk, 카테고리 이름 정보")
            }
    )
    @Operation(summary = "하위 카테고리 수정", description = "소분류 카테고리를 수정한다")
    @PutMapping("/{categoryId}")
    public ResponseEntity<?> updateCategory(@PathVariable Long categoryId, @RequestBody CategoryRequest.UpdateCategoryRequest request) {

        /**
         * TODO : jwt
         *    권한 없음
         *    존재하지 않는 상위 카테고리
         *    이미 존재하는 카테고리 이름
         */
        categoryAppService.updateCategory(request.toCommand(categoryId));

        return new ResponseEntity<>(request, HttpStatus.OK);
    }

    @Parameter(name = "categoryId", description = "삭제할 카테고리 pk")
    @Operation(summary = "하위 카테고리 삭제", description = "소분류 카테고리를 삭제한다.")
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long categoryId) {

        /**
         * TODO : jwt
         *    권한 없음
         *    대분류 삭제시 하위 카테고리 없어야함
         */
        categoryAppService.deleteCategory(categoryId);

        return new ResponseEntity<>(categoryId, HttpStatus.OK);
    }

    @PostMapping("/migrate")
    public ResponseEntity<?> migrateCategory(@RequestParam Long from,@RequestParam Long to){
        categoryAppService.migrateCategory(from,to);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}