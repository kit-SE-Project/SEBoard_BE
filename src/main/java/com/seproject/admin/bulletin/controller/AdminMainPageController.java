package com.seproject.admin.bulletin.controller;

import com.seproject.admin.bulletin.application.AdminMainPageAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.seproject.admin.bulletin.controller.dto.MainPageDTO.*;

@Tag(name = "메인페이지 관리 API", description = "관리자 시스템의 메인페이지 관리 API")
@AllArgsConstructor
@RequestMapping(value = "/admin/mainPageMenus")
@RestController
public class AdminMainPageController {

    private final AdminMainPageAppService adminMainPageAppService;

    @Operation(summary = "메인 페이지에 보여줄 메뉴 조회", description = "메인 페이지에 보여줄 메뉴로 설정된 정보를 조회한다.")
    @GetMapping
    public ResponseEntity<List<MainPageMenuResponse>> retrieveAllMainPageMenus() {
        List<MainPageMenuResponse> response = adminMainPageAppService.retrieveAllMainPageMenus();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "메인 페이지에 등록 가능한 메뉴 목록을 조회한다.", description = "메인 페이지에 등록 가능한 메뉴의 정보를 조회한다.(메인 페이지에 보여줄 메뉴  조회 기능과 통합될 수 있음)")
    @GetMapping("/all") // TODO :
    public ResponseEntity<List<InternalSiteMenuResponse>> retrieveAllInternalSiteMenu() {
        List<InternalSiteMenuResponse> response = adminMainPageAppService.retrieveAllInternalSiteMenu();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "메인 페이지에 보여줄 메뉴 추가", description = "메인 페이지에 보여줄 메뉴를 추가한다.")
    @PostMapping
    public ResponseEntity<Void> addMainPageMenus(@RequestBody CreateMainPageMenuRequest request) {
        adminMainPageAppService.createMainPageMenu(request.getMenuId());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "메인 페이지에 보여줄 메뉴 삭제", description = "메인 페이지에 보여줄 메뉴를 삭제한다.")
    @DeleteMapping
    public ResponseEntity<Void> deleteMainPageMenus(@RequestBody DeleteMainPageMenuRequest request) {
        adminMainPageAppService.deleteMainPageMenu(request.getId());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "메인 페이지에 보여줄 메뉴 업데이트", description = "메인 페이지에 보여줄 메뉴를 수정한다.")
    @PutMapping
    public ResponseEntity<Void> updateMainPageMenus(@RequestBody UpdateMainPageMenuRequest request) {
        adminMainPageAppService.updateMainPageMenu(request.getMenuIds());
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
